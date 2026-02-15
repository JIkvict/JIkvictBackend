package org.jikvict.jikvictbackend.service

import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.model.dto.UserDto
import org.jikvict.jikvictbackend.model.mapper.UserMapper
import org.jikvict.jikvictbackend.repository.UserRepository
import org.jikvict.jikvictbackend.service.auth.LdapAuthenticationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ImportResult(
    val username: String,
    val success: Boolean,
    val message: String,
)

@Service
class UserService(
    private val ldapAuthenticationService: LdapAuthenticationService,
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
) {
    @Transactional
    fun getUsersOfGroups(groupIds: List<Long>): List<UserDto> =
        userRepository.findDistinctByAssignmentGroups_IdIn(groupIds).map {
            userMapper.toUserDto(it)
        }

    @Transactional
    fun getAllUsers(): List<UserDto> =
        userRepository.findAll().map {
            userMapper.toUserDto(it)
        }

    @Transactional
    fun getUserById(id: Long): UserDto? =
        userRepository
            .findById(id)
            .map {
                userMapper.toUserDto(it)
            }.orElse(null)

    @Transactional
    fun importUser(
        username: String,
        key: String = "uisId",
    ): ImportResult {
        if (username.isBlank()) {
            return ImportResult(username, false, "Username cannot be blank")
        }

        val ldapUserData = ldapAuthenticationService.getUserData(username, key) ?: return ImportResult(username, false, "User not found in LDAP")

        val existingUser = userRepository.findUserByUserNameField(username)

        return if (existingUser != null) {
            existingUser.email = ldapUserData.email
            existingUser.aisId = ldapUserData.aisId
            userRepository.save(existingUser)
            ImportResult(username, true, "User updated successfully")
        } else {
            val newUser =
                User().apply {
                    userNameField = username
                    email = ldapUserData.email
                    aisId = ldapUserData.aisId
                }
            userRepository.save(newUser)
            ImportResult(username, true, "User created successfully")
        }
    }

    @Transactional
    fun importUsers(usernames: List<String>): List<ImportResult> = usernames.map { username -> importUser(username) }

    @Transactional
    fun importUserEntityByAisId(aisId: String): User? {
        if (aisId.isBlank()) {
            return null
        }

        val ldapUserData =
            ldapAuthenticationService.getUserData(aisId, "uid")
                ?: return null

        val existingUser = userRepository.findByAisId(aisId)

        return if (existingUser != null) {
            existingUser.email = ldapUserData.email
            existingUser.aisId = ldapUserData.aisId
            userRepository.save(existingUser)
        } else {
            val newUser =
                User().apply {
                    userNameField = ldapUserData.username
                    email = ldapUserData.email
                    this.aisId = ldapUserData.aisId
                }
            userRepository.save(newUser)
        }
    }

    fun userExistsInLdap(username: String): Boolean = ldapAuthenticationService.userExists(username)
}
