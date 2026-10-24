// movie-service: миграция на Boot 2.3 / Hoxton / Java 11 (стадия B1 — только сборка,
// Java-код будет мигрирован на стадиях B2–B4, компиляция ожидаемо падает).

plugins {
    id("org.openapi.generator") version "7.10.0"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.mapstruct:mapstruct:1.6.3")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:Hoxton.SR12")
    }
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$rootDir/openapi/movie-service.yaml")
    outputDir.set("$buildDir/generated")
    apiPackage.set("ru.itmo.soa.movie.api")
    modelPackage.set("ru.itmo.soa.movie.dto")
    configOptions.set(mapOf(
        "interfaceOnly" to "true",
        "useTags" to "true",
        "useSpringBoot3" to "false",
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

tasks.named<Jar>("jar") {
    archiveFileName.set("movie-service.jar")
}
