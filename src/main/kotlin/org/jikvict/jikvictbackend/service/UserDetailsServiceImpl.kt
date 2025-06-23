package org.jikvict.jikvictbackend.service

import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user =
            userRepository.findUserByUserNameField(username)
                ?: throw UsernameNotFoundException("User not found with username: $username")
        return User().apply {
            userNameField = user.username
            userPassword = user.password
            roles.addAll(user.roles)
        }
    }
}
