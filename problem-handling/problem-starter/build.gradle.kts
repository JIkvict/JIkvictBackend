import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("jikvict-common-conventions")
    id("jikvict-spring-conventions")
}

group = "org.jikvict.problem.handling"

dependencies {
    api(project(":problem-handling:problem-library"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    testImplementation(libs.bundles.test)
}
tasks.named<BootJar>("bootJar") {
    enabled = false
}
