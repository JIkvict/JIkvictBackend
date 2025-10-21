package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.Role
import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: String): Role?
}
