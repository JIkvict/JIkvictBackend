import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask

plugins {
    id("jikvict-common-conventions")
    id("org.jlleitschuh.gradle.ktlint")
}

configure<KtlintExtension> {
    coloredOutput.set(true)
    version.set("1.6.0")
    reporters {
        reporter(ReporterType.CHECKSTYLE)
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
