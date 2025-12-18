package org.jikvict.jikvictbackend.service.filter

import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.service.token.JwtService
import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class JwtWebSocketHandshakeInterceptor(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService,
) : HandshakeInterceptor {
    private val logger = LoggerFactory.getLogger(JwtWebSocketHandshakeInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        if (request is ServletServerHttpRequest) {
            val servletRequest = request.servletRequest
            val queryString = servletRequest.queryString

            if (queryString != null) {
                val token = extractTokenFromQuery(queryString)
                if (token != null) {
                    try {
                        val username = jwtService.extractUsername(token)
                        val userDetails = userDetailsService.loadUserByUsername(username)

                        if (jwtService.isTokenValid(token, userDetails as User)) {
                            val authToken =
                                UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.authorities,
                                )
                            SecurityContextHolder.getContext().authentication = authToken
                            attributes["username"] = username
                            logger.debug("WebSocket authentication successful for user: $username")
                        } else {
                            logger.warn("Invalid token provided for WebSocket connection")
                        }
                    } catch (ex: Exception) {
                        logger.error("Failed to authenticate WebSocket connection: ${ex.message}")
                        SecurityContextHolder.clearContext()
                    }
                }
            }
        }

        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) {
        // Nothing to do after handshake
    }

    private fun extractTokenFromQuery(queryString: String): String? {
        val params = queryString.split("&")
        for (param in params) {
            val keyValue = param.split("=", limit = 2)
            if (keyValue.size == 2 && keyValue[0] == "access_token") {
                return keyValue[1]
            }
        }
        return null
    }
}
