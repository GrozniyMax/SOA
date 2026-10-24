// Общий (корневой) build.gradle.kts: версии плагинов и общая конфигурация для всех модулей.
plugins {
    java
    id("io.spring.dependency-management") version "1.1.7" apply false
    // Внимание: плагин Spring Boot НЕ объявляем в корне — модули используют
    // разные версии (3.5.3 для новых модулей, 2.3.12.RELEASE для api-gateway),
    // а корневой plugins-блок кладёт версию на общий classpath и конфликтует.
}

allprojects {
    group = "ru.itmo.soa"
    version = "0.0.1-SNAPSHOT"
}

// «Legacy island»: модули на старом стеке (Boot 2.3 + Cloud Hoxton + Java 11),
// к ним корневая конфигурация Java 21 не применяется.
val legacyModules = setOf("api-gateway")

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(if (project.name in legacyModules) 11 else 21)
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}