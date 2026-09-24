// movie-service: WAR для развёртывания на Payara (Payara Micro для локального запуска).
import java.net.URI
import org.springframework.boot.gradle.tasks.bundling.BootWar

plugins {
    war
    id("org.openapi.generator") version "7.10.0"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    // Embedded Tomcat не попадает в WAR — сервис разворачивается на Payara.
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// Генерация API-интерфейса и DTO из OpenAPI-спецификации.
openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$rootDir/openapi/movie-service.yaml")
    outputDir.set("$buildDir/generated")
    apiPackage.set("ru.tbank.soa.movie.api")
    modelPackage.set("ru.tbank.soa.movie.dto")
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
    // Базовый `Page` объявляет `items` без типа элемента (типизируется через allOf).
    // Строгая валидация спецификации ошибочно считает это ошибкой, поэтому отключаем её.
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

// Версия Payara Micro, которой запускается собранный WAR локально.
val payaraMicroVersion = "6.2025.4"
val payaraMicroJar = layout.buildDirectory.file("libs/payara-micro.jar")

tasks.register("downloadPayaraMicro") {
    group = "application"
    description = "Скачивает Payara Micro для локального запуска WAR."
    outputs.file(payaraMicroJar)
    doLast {
        val target = payaraMicroJar.get().asFile
        if (!target.exists()) {
            target.parentFile.mkdirs()
            val url = URI(
                "https://repo1.maven.org/maven2/fish/payara/extras/payara-micro/" +
                    "$payaraMicroVersion/payara-micro-$payaraMicroVersion.jar"
            )
            url.toURL().openStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}

tasks.register("runPayara") {
    group = "application"
    description = "Запускает movie-service на embedded Payara Micro (java -jar payara-micro.jar --deploy movie.war)."
    dependsOn("bootWar", "downloadPayaraMicro")
    doLast {
        val war = tasks.named<BootWar>("bootWar").get().archiveFile.get().asFile
        val jar = payaraMicroJar.get().asFile
        providers.exec {
            commandLine("java", "-jar", jar.absolutePath, "--deploy", war.absolutePath)
        }.result.get()
    }
}