plugins {
    id("jikvict-common-conventions")
    id("jikvict-spring-conventions")
    id("org.springdoc.openapi-gradle-plugin")
}

openApi {
    outputDir.set(file("${layout.buildDirectory.get()}/openapi"))
    outputFileName.set("openapi.json")
    apiDocsUrl.set("http://localhost:8080/v3/api-docs")
    waitTimeInSeconds.set(40)
}
