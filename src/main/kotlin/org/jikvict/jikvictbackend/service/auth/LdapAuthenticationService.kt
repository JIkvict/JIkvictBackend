package org.jikvict.jikvictbackend.service.auth

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
    private val ldapUrl = "ldaps://ldap.stuba.sk:636"
    private val baseDn = "ou=People,dc=stuba,dc=sk"

    fun authenticate(
        username: String,
        password: String,
    ): Boolean {
        if (username.isBlank() || password.isBlank()) {
            return false
        }

        val userDn = "uid=$username,$baseDn"
        val env = Hashtable<String, String>()

        env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
        env[Context.PROVIDER_URL] = ldapUrl
        env[Context.SECURITY_AUTHENTICATION] = "simple"
        env[Context.SECURITY_PRINCIPAL] = userDn
        env[Context.SECURITY_CREDENTIALS] = password

        return try {
            val context = InitialDirContext(env)
            context.close()
            true
        } catch (e: Exception) {
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
    ): LdapUserData? {
        if (username.isBlank()) {
            return null
        }

        val env = Hashtable<String, String>()
        env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
        env[Context.PROVIDER_URL] = ldapUrl
        env[Context.SECURITY_AUTHENTICATION] = "none"

        return try {
            val context = InitialDirContext(env)
            val searchControls = SearchControls()
            searchControls.searchScope = SearchControls.SUBTREE_SCOPE
            searchControls.returningAttributes = arrayOf("mail", "uisId")

            val results = context.search(baseDn, "$key=$username", searchControls)

            if (results.hasMore()) {
                val result = results.next()
                val attributes = result.attributes

                val email = attributes[("mail")]?.get()?.toString() ?: "unknown"
                val aisId = attributes[("uisId")]?.get()?.toString() ?: "unknown"

                context.close()
                LdapUserData(username, email, aisId)
            } else {
                context.close()
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
