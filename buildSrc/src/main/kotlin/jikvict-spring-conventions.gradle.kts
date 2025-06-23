plugins {
    id("jikvict-common-conventions")
    id("org.jetbrains.kotlin.plugin.spring")
    id("io.spring.dependency-management")
    id("org.springframework.boot")
    id("org.jetbrains.kotlin.plugin.jpa")
}
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
