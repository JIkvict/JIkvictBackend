plugins {
    id("jikvict-common-conventions")
    id("jikvict-spring-conventions")
}


dependencies {
    api(project(":problem-handling:library"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.bundles.test)
}
