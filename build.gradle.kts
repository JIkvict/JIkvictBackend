plugins {
    id("jikvict-common-conventions")
    id("jikvict-spring-conventions")
    id("jikvict-openapi-conventions")
    id("jikvict-ktlint-conventions")
    id("idea")
    kotlin("kapt")
}

dependencies {
    implementation(project(":problem-handling:starter"))
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.kotlin.reflect)
    implementation(libs.jjwt.api)
    implementation(libs.testcontainers)
    implementation(libs.jgit)
    implementation(libs.spring.boot.starter.amqp)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.security.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.bundles.test)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jikvict.testing.library)
    kapt("org.mapstruct:mapstruct-processor:1.6.0")
    compileOnly("org.mapstruct:mapstruct:1.6.0")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
