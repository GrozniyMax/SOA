import org.springframework.boot.gradle.tasks.bundling.BootWar

plugins {
    war
    id("org.openapi.generator") version "7.10.0"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.mapstruct:mapstruct:1.6.3")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$rootDir/openapi/oscar-service.yaml")
    outputDir.set("$buildDir/generated")
    apiPackage.set("ru.tbank.soa.oscar.api")
    modelPackage.set("ru.tbank.soa.oscar.dto")
    configOptions.set(mapOf(
        "interfaceOnly" to "true",
        "useTags" to "true",
        "useSpringBoot3" to "true",
        "useBeanValidation" to "true",
        "openApiNullable" to "false",
        "generateApiTests" to "false",
        "generateModelTests" to "false",
        "generateApiDocumentation" to "false",
        "generateModelDocumentation" to "false",
        "documentationProvider" to "none",
        "library" to "spring-boot"
    ))
    validateSpec.set(false)
    skipValidateSpec.set(true)
    typeMappings.set(mapOf(
        "OffsetDateTime" to "java.time.LocalDate",
        "LocalDateTime" to "java.time.LocalDate",
        "LocalDate" to "java.time.LocalDate",
        "Date" to "java.time.LocalDate"
    ))
}

sourceSets {
    main {
        java.srcDir("$buildDir/generated/src/main/java")
    }
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

tasks.named<BootWar>("bootWar") {
    archiveFileName.set("oscar.war")
}
