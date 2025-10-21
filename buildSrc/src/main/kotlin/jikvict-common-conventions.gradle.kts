import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("idea")
    id("pl.allegro.tech.build.axion-release")
}


group = "org.jikvict"
version = scmVersion.version

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        freeCompilerArgs.add("-Xcontext-parameters")
        jvmTarget.set(JvmTarget.JVM_21)
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
    }
}


kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}


plugins.withId("java") {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

val deployVersion = providers.gradleProperty("deployVersion")
val deployHost = providers.gradleProperty("deployHost").orElse("147.175.151.161")
val deployUser = providers.gradleProperty("deployUser").orElse("ubuntu")
val deployKeyPath = providers.gradleProperty("deployKeyPath").orElse("~/.ssh/id_rsa-fiit")
val deployDir = providers.gradleProperty("deployDir").orElse("~/jikvict")

tasks.register<Exec>("deploy") {
    group = "deployment"
    description = "Deploys specified backend image version to remote host via SSH (edits compose.yaml and runs docker compose up -d)."

    doFirst {
        val version = deployVersion.orNull ?: throw GradleException("Missing -PdeployVersion=x.y.z")
        val host = deployHost.get()
        val user = deployUser.get()
        val keyPath = deployKeyPath.get()
        val remoteDir = deployDir.get()

        println("[deploy] Host=$host, User=$user, Dir=$remoteDir, Version=$version")
        println("[deploy] Using key: $keyPath")

        // language=shell script
        val remoteScript = """
            set -euo pipefail
            cd $remoteDir
            echo "Creating backup compose.yaml.bak..."
            cp compose.yaml compose.yaml.bak || true
            VERSION="$version"
            echo "Updating backend image tag to: $version"
            sed -i -E "s|(image:[[:space:]]*ikvict/jikvict-backend:)[^[:space:]]+|\1$version|" compose.yaml
            echo "Resulting image line:"
            grep -nE 'image:\s*ikvict/jikvict-backend:' compose.yaml || true
            echo "Bringing up services with new image..."
            if command -v compose >/dev/null 2>&1; then
              sudo compose up -d
            else
              sudo docker compose up -d
            fi
            echo "Deployment completed."
        """.trimIndent()

        commandLine(
            "ssh",
            "-o", "StrictHostKeyChecking=no",
            "-i", keyPath,
            "$user@$host",
            remoteScript,
        )
    }
}
