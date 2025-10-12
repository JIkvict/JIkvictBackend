package org.jikvict.jikvictbackend.service.auth

import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.repository.UserRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LdapAuthenticationProvider(
    private val ldapAuthenticationService: LdapAuthenticationService,
    private val userRepository: UserRepository,
) : AuthenticationProvider {

    @Transactional
    override fun authenticate(authentication: Authentication): Authentication {
        val username = authentication.name
        val password = authentication.credentials.toString()

        if (!ldapAuthenticationService.authenticate(username, password)) {
            throw BadCredentialsException("Invalid username or password")
        }

        val ldapUserData = ldapAuthenticationService.getUserData(username)

        val user = userRepository.findUserByUserNameField(username)
            ?.also { updateUserFromLdap(it, ldapUserData) }
            ?: createUser(username, ldapUserData)

        return UsernamePasswordAuthenticationToken(user, password, user.authorities)
    }

    override fun supports(authentication: Class<*>): Boolean {
        return authentication == UsernamePasswordAuthenticationToken::class.java
    }

    private fun createUser(username: String, ldapUserData: LdapUserData?): User {
        val user = User().apply {
            userNameField = username
            email = ldapUserData?.email
            aisId = ldapUserData?.aisId
        }
        return userRepository.save(user)
    }

    private fun updateUserFromLdap(user: User, ldapUserData: LdapUserData?): User {
        user.email = ldapUserData?.email
        user.aisId = ldapUserData?.aisId
        return userRepository.save(user)
    }
}
