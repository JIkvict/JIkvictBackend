
package org.jikvict.jikvictbackend.service.auth

import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.util.Hashtable
import javax.naming.Context
import javax.naming.directory.InitialDirContext
import javax.naming.directory.SearchControls

data class LdapUserData(
    val username: String,
    val email: String,
    val aisId: String,
)

@Service
class LdapAuthenticationService {
    private val logger = LogManager.getLogger(this::class.java)
    private val ldapUrl = "ldaps://ldap.stuba.sk:636"
    private val baseDn = "ou=People,dc=stuba,dc=sk"

    fun authenticate(
        username: String,
        password: String,
    ): Boolean {
        if (username.isBlank() || password.isBlank()) {
            logger.warn("Authentication failed: username or password is blank")
            return false
        }

        val userDn = "uid=$username,$baseDn"
        logger.info("Attempting LDAP authentication for user: $username with DN: $userDn")
        val env = Hashtable<String, String>()

        env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
        env[Context.PROVIDER_URL] = ldapUrl
        env[Context.SECURITY_AUTHENTICATION] = "simple"
        env[Context.SECURITY_PRINCIPAL] = userDn
        env[Context.SECURITY_CREDENTIALS] = password

        return try {
            val context = InitialDirContext(env)
            context.close()
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

        val env = Hashtable<String, String>()
        env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
        env[Context.PROVIDER_URL] = ldapUrl
        env[Context.SECURITY_AUTHENTICATION] = "none"

        return try {
            val context = InitialDirContext(env)
            val searchControls = SearchControls()
            searchControls.searchScope = SearchControls.SUBTREE_SCOPE

            val results = context.search(baseDn, "uid=$username", searchControls)
            val exists = results.hasMore()
            context.close()
            exists
        } catch (e: Exception) {
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
        val env = Hashtable<String, String>()
        env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
        env[Context.PROVIDER_URL] = ldapUrl

        if (authUsername != null && authPassword != null) {
            env[Context.SECURITY_AUTHENTICATION] = "simple"
            env[Context.SECURITY_PRINCIPAL] = "uid=$authUsername,$baseDn"
            env[Context.SECURITY_CREDENTIALS] = authPassword
            logger.debug("Using authenticated search")
        } else {
            env[Context.SECURITY_AUTHENTICATION] = "none"
            logger.debug("Using anonymous search")
        }

        return try {
            val context = InitialDirContext(env)
            val searchControls = SearchControls()
            searchControls.searchScope = SearchControls.SUBTREE_SCOPE
            searchControls.returningAttributes = arrayOf("mail", "uisId", "uid")

            val results = context.search(baseDn, "$key=$username", searchControls)

            if (results.hasMore()) {
                val result = results.next()
                val attributes = result.attributes

                val email = attributes[("mail")]?.get()?.toString()
                val aisId = attributes[("uisId")]?.get()?.toString()
                val aisName = attributes[("uid")]?.get()?.toString()

                logger.info("Found user data: email=$email, aisId=$aisId, aisName=$aisName")

                if (email == null || aisId == null || aisName == null) {
                    logger.warn("Incomplete user data for: $username")
                    context.close()
                    return null
                }

                context.close()
                LdapUserData(aisName, email, aisId)
            } else {
                context.close()
                logger.warn("No user data found for: $username")
                null
            }
        } catch (e: Exception) {
            logger.error("getUserData failed for user: $username - ${e.message}", e)
            null
        }
    }
}
