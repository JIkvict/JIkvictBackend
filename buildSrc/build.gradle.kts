plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.1.21")
    implementation("org.jetbrains.kotlin:kotlin-noarg:2.1.21")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.5.3")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
    implementation("org.springdoc:springdoc-openapi-gradle-plugin:1.8.0")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.3.0")
}
