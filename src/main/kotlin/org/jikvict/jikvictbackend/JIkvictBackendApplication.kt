package org.jikvict.jikvictbackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@SpringBootApplication
@EnableMethodSecurity
class JIkvictBackendApplication

fun main(args: Array<String>) {
    runApplication<JIkvictBackendApplication>(*args)
}
