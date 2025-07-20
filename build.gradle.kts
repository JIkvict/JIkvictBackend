plugins {
    id("jikvict-common-conventions")
    id("jikvict-spring-conventions")
    id("jikvict-openapi-conventions")
    id("jikvict-ktlint-conventions")
    id("idea")
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
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
