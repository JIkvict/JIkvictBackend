
package org.jikvict.jikvictbackend.service.auth

import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

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

    fun authenticate(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) {
            logger.warn("Authentication failed: username or password is blank")
            return false
        }

        logger.info("Attempting LDAP authentication for user: $username via ldapsearch")

        return try {
            val bindDn = "uid=$username,$baseDn"

            val process = ProcessBuilder(
                "ldapsearch",
                "-x",
                "-H", ldapUrl,
                "-D", bindDn,
                "-w", password,
                "-b", baseDn,
                "(uid=$username)",
                "uid"
            ).redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor(10, TimeUnit.SECONDS)

            if (!exitCode) {
                logger.error("LDAP authentication timeout for user: $username")
                process.destroyForcibly()
                return false
            }

            val result = process.exitValue()

            if (result == 0) {
                logger.info("LDAP authentication successful for user: $username")
                true
            } else {
                logger.warn("LDAP authentication failed for user: $username, exit code: $result")
                false
            }
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
            val process = ProcessBuilder(
                "ldapsearch",
                "-x",
                "-H", ldapUrl,
                "-b", baseDn,
                "(uid=$username)",
                "uid"
            ).redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor(10, TimeUnit.SECONDS)

            if (!exitCode) {
                process.destroyForcibly()
                return false
            }

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }

            process.exitValue() == 0 && output.contains("uid: $username")
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
            val commands = mutableListOf(
                "ldapsearch",
                "-x",
                "-H", ldapUrl
            )

            if (authUsername != null && authPassword != null) {
                val bindDn = "uid=$authUsername,$baseDn"
                commands.addAll(listOf("-D", bindDn, "-w", authPassword))
            }

            commands.addAll(listOf(
                "-b", baseDn,
                "($key=$username)",
                "mail", "uisId", "uid"
            ))

            val process = ProcessBuilder(commands)
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor(10, TimeUnit.SECONDS)

            if (!exitCode) {
                logger.error("LDAP getUserData timeout for user: $username")
                process.destroyForcibly()
                return null
            }

            if (process.exitValue() != 0) {
                logger.warn("LDAP getUserData failed for user: $username, exit code: ${process.exitValue()}")
                return null
            }

            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }

            parseLdapOutput(output)?.also {
                logger.info("Found user data: ${it.email}, ${it.aisId}, ${it.username}")
            } ?: run {
                logger.warn("No user data found for: $username")
                null
            }
        } catch (e: Exception) {
            logger.error("getUserData failed for user: $username - ${e.message}", e)
            null
        }
    }

    private fun parseLdapOutput(output: String): LdapUserData? {
        val lines = output.lines()

        var username: String? = null
        var email: String? = null
        var aisId: String? = null

        for (line in lines) {
            when {
                line.startsWith("uid: ") -> username = line.substringAfter("uid: ").trim()
                line.startsWith("mail: ") -> email = line.substringAfter("mail: ").trim()
                line.startsWith("uisId: ") -> aisId = line.substringAfter("uisId: ").trim()
            }
        }

        return if (username != null && email != null && aisId != null) {
            LdapUserData(username, email, aisId)
        } else {
            null
        }
    }
}
