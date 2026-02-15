
package org.jikvict.jikvictbackend.service.auth

import org.apache.logging.log4j.LogManager
import org.springframework.boot.autoconfigure.ldap.LdapProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ldap.core.AttributesMapper
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.ldap.filter.EqualsFilter
import org.springframework.ldap.query.LdapQueryBuilder
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.naming.directory.Attributes
import javax.net.SocketFactory
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.jvm.java

data class LdapUserData(
    val username: String,
    val email: String,
    val aisId: String,
)

class TrustAllSSLSocketFactory {
    companion object {
        private val sslSocketFactory: SSLSocketFactory

        init {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                    override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
                },
            )

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            sslSocketFactory = sslContext.socketFactory
        }

        @JvmStatic
        fun getDefault(): SocketFactory = sslSocketFactory
    }
}

@Configuration
class LdapConfig {

    init {
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                    override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
                },
            )

            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, SecureRandom())
            SSLContext.setDefault(sc)
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

            System.setProperty("https.protocols", "TLSv1.2,TLSv1.3")
            System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.3")

            LogManager.getLogger(this::class.java).warn("Global SSL validation disabled for LDAP")
        } catch (e: Exception) {
            LogManager.getLogger(this::class.java).error("Failed to disable SSL validation", e)
        }
    }

    @Bean
    fun ldapContextSource(properties: LdapProperties): LdapContextSource {
        val contextSource = LdapContextSource()
        contextSource.setUrl("ldaps://ldap.stuba.sk:636")
        contextSource.setBase("ou=People,dc=stuba,dc=sk")
        contextSource.isAnonymousReadOnly = true
        contextSource.afterPropertiesSet()
        properties.baseEnvironment["java.naming.ldap.factory.socket"] = TrustAllSSLSocketFactory::class.java.getName()
        contextSource.setBaseEnvironmentProperties(properties.baseEnvironment.toMap())
        return contextSource
    }

    @Bean
    fun ldapTemplate(contextSource: LdapContextSource): LdapTemplate {
        return LdapTemplate(contextSource)
    }
}

@Service
class LdapAuthenticationService(
    private val ldapTemplate: LdapTemplate,
) {
    private val logger = LogManager.getLogger(this::class.java)

    fun authenticate(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) {
            logger.warn("Authentication failed: username or password is blank")
            return false
        }

        logger.info("Attempting LDAP authentication for user: $username")

        return try {
            ldapTemplate.authenticate(
                "",
                EqualsFilter("uid", username).encode(),
                password,
            )
            logger.info("LDAP authentication successful for user: $username")
            true
        } catch (e: Exception) {
            logger.error("LDAP authentication failed for user: $username - ${e.message}", e)
            false
        }
    }

    fun userExists(username: String): Boolean {
        if (username.isBlank()) {
            return false
        }

        return try {
            val query = LdapQueryBuilder.query()
                .where("uid").`is`(username)

            val results = ldapTemplate.search(
                query,
                AttributesMapper<String> { attrs ->
                    attrs["uid"]?.get()?.toString()
                },
            ).toList()

            results.isNotEmpty()
        } catch (e: Exception) {
            logger.error("Error checking if user exists: $username - ${e.message}", e)
            false
        }
    }

    fun getUserData(
        username: String,
        key: String = "uid",
        authUsername: String? = null,
        authPassword: String? = null,
    ): LdapUserData? {
        if (username.isBlank()) {
            logger.warn("getUserData failed: username is blank")
            return null
        }

        logger.info("Getting user data for: $username (auth: ${authUsername != null})")

        return try {
            val template = if (authUsername != null && authPassword != null) {
                val authContextSource = LdapContextSource().apply {
                    setUrl("ldaps://ldap.stuba.sk:636")
                    setBase("ou=People,dc=stuba,dc=sk")
                    userDn = "uid=$authUsername,ou=People,dc=stuba,dc=sk"
                    setPassword(authPassword)
                    afterPropertiesSet()
                }
                LdapTemplate(authContextSource)
            } else {
                ldapTemplate
            }

            val query = LdapQueryBuilder.query()
                .where(key).`is`(username)

            val results = template.search(query) { attrs: Attributes ->
                val email = attrs["mail"]?.get()?.toString()
                val aisId = attrs["uisId"]?.get()?.toString()
                val aisName = attrs["uid"]?.get()?.toString()

                if (email != null && aisId != null && aisName != null) {
                    LdapUserData(aisName, email, aisId)
                } else {
                    null
                }
            }.toList()

            val userData = results.firstOrNull()

            if (userData != null) {
                logger.info("Found user data: ${userData.email}, ${userData.aisId}, ${userData.username}")
            } else {
                logger.warn("No user data found for: $username")
            }

            userData
        } catch (e: Exception) {
            logger.error("getUserData failed for user: $username - ${e.message}", e)
            null
        }
    }
}
