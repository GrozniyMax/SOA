import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.5.3"
    id("org.openapi.generator") version "7.10.0"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("io.github.openfeign:feign-okhttp")
    implementation("io.github.openfeign:feign-jackson")
    implementation("io.github.openfeign:feign-form")
    implementation("io.github.openfeign:feign-slf4j")
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
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
    }
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$rootDir/openapi/oscar-service.yaml")
    outputDir.set("$buildDir/generated")
    apiPackage.set("ru.itmo.soa.oscar.api")
    modelPackage.set("ru.itmo.soa.oscar.dto")
    invokerPackage.set("ru.itmo.soa.oscar")
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
        java.srcDir("$buildDir/generated-movie-feign/src/main/java")
    }
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate", "generateMovieFeignClient")
}

// Генерация Feign-клиента из спецификации movie-service (для межсервисных вызовов).
val generateMovieFeignClient = tasks.register<GenerateTask>("generateMovieFeignClient") {
    generatorName.set("java")
    library.set("feign")
    inputSpec.set("$rootDir/openapi/movie-service.yaml")
    outputDir.set("$buildDir/generated-movie-feign")
    apiPackage.set("ru.itmo.soa.movie.client")
    modelPackage.set("ru.itmo.soa.movie.dto")
    invokerPackage.set("ru.itmo.soa.movie")
    configOptions.set(mapOf(
        "dateLibrary" to "java8",
        "useJakartaEe" to "true",
        "openApiNullable" to "false",
        "generateApiTests" to "false",
        "generateModelTests" to "false",
        "generateApiDocumentation" to "false",
        "generateModelDocumentation" to "false"
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

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("oscar.jar")
}
