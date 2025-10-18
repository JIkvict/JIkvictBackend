import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("jikvict-common-conventions")
    id("jikvict-spring-conventions")
}

group = "org.jikvict.docker"

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.reflect)
    implementation(libs.testcontainers)
    testImplementation(libs.bundles.test)
}
tasks.named<BootJar>("bootJar") {
    enabled = false
}
