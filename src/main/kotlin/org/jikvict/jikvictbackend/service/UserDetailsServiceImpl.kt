package org.jikvict.jikvictbackend.service

import org.jikvict.jikvictbackend.entity.User
import org.jikvict.jikvictbackend.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
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
        return user
    }

    fun getCurrentUser(): User = loadUserByUsername(SecurityContextHolder.getContext().authentication.name) as User
}
