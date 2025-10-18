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
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allOpen)
    implementation(libs.kotlin.noArgs)
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.spring.gradle.dependency.management)
    implementation(libs.spring.openapi.gradle.plugin)
    implementation(libs.ktlint)
    implementation(libs.axion.release)
}
