package org.jikvict.jikvictbackend.service

import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.repository.UserRepository
import org.jikvict.jikvictbackend.service.auth.LdapAuthenticationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ImportResult(
    val username: String,
    val success: Boolean,
    val message: String
)

@Service
class UserImportService(
    private val ldapAuthenticationService: LdapAuthenticationService,
    private val userRepository: UserRepository
) {

    @Transactional
    fun importUser(username: String, key: String = "uisId"): ImportResult {
        if (username.isBlank()) {
            return ImportResult(username, false, "Username cannot be blank")
        }

        val ldapUserData = ldapAuthenticationService.getUserData(username,key) ?: return ImportResult(username, false, "User not found in LDAP")

        val existingUser = userRepository.findUserByUserNameField(username)

        return if (existingUser != null) {
            existingUser.email = ldapUserData.email
            existingUser.aisId = ldapUserData.aisId
            userRepository.save(existingUser)
            ImportResult(username, true, "User updated successfully")
        } else {
            val newUser = User().apply {
                userNameField = username
                email = ldapUserData.email
                aisId = ldapUserData.aisId
            }
            userRepository.save(newUser)
            ImportResult(username, true, "User created successfully")
        }
    }

    @Transactional
    fun importUsers(usernames: List<String>): List<ImportResult> {
        return usernames.map { username -> importUser(username) }
    }

    @Transactional
    fun importUserEntity(username: String): User? {
        if (username.isBlank()) {
            return null
        }

        val ldapUserData = ldapAuthenticationService.getUserData(username)
            ?: return null

        val existingUser = userRepository.findUserByUserNameField(username)

        return if (existingUser != null) {
            existingUser.email = ldapUserData.email
            existingUser.aisId = ldapUserData.aisId
            userRepository.save(existingUser)
        } else {
            val newUser = User().apply {
                userNameField = username
                email = ldapUserData.email
                aisId = ldapUserData.aisId
            }
            userRepository.save(newUser)
        }
    }

    fun userExistsInLdap(username: String): Boolean {
        return ldapAuthenticationService.userExists(username)
    }
}
