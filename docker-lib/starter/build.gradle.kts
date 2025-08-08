plugins {
    id("jikvict-common-conventions")
    id("jikvict-spring-conventions")
}

group = "org.jikvict.docker.starter"

dependencies {
    api(project(":docker-lib:library"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    testImplementation(libs.bundles.test)
}
