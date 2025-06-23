import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.springdoc.openapi-gradle-plugin")
    id("org.jlleitschuh.gradle.ktlint")
}


group = "org.jikvict"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xjsr305=strict"))
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

openApi {
    outputDir.set(file("${layout.buildDirectory.get()}/openapi"))
    outputFileName.set("openapi.json")
    apiDocsUrl.set("http://localhost:8080/v3/api-docs")
    waitTimeInSeconds.set(10)
}

configure<KtlintExtension> {
    coloredOutput.set(true)
    version.set("1.6.0")
    reporters {
        reporter(CHECKSTYLE)
    }
    filter {
        exclude { it.isGeneratedSource() }
        exclude { element -> element.file.path.contains("build.gradle.kts") }
        exclude("**/build.gradle.kts")

    }
}

tasks.withType<KtLintCheckTask> {
    dependsOn("ktlintFormat")
}

private fun FileTreeElement.isGeneratedSource(): Boolean {
    return file.path.contains("generated-sources/")
}
