// Общий (корневой) build.gradle.kts: единый стек Spring Boot 2.3 / Spring Cloud Hoxton / Java 11.
plugins {
    java
    id("io.spring.dependency-management") version "1.1.7" apply false
    // Boot-плагин 2.3 несовместим с Gradle 8.14 — не применяем; версии пинятся BOM'ом Hoxton.
}

allprojects {
    group = "ru.itmo.soa"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    // Единый BOM для всего проекта — в модулях не дублируется.
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            // dependency-management не раскрывает цепочку BOM'ов — импортируем явно.
            mavenBom("org.springframework.boot:spring-boot-dependencies:2.3.12.RELEASE")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:Hoxton.SR12")
            // Пиним feign 10.12 явно (иначе уедет в 13.x).
            mavenBom("io.github.openfeign:feign-bom:10.12")
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(11)
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // Аналог record-компонентов: имена параметров конструкторов нужны MapStruct
    // (конструкторный маппинг immutable-классов) и Spring MVC для @RequestParam.
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
    }

    // Hoxton пинит gson 2.1, но eureka-client требует GsonBuilder.setLenient (2.2+).
    configurations.all {
        resolutionStrategy {
            force("com.google.code.gson:gson:2.8.6")
        }
    }
    // Classpath-дистрибутив (не fat-jar): склейка ломает spring.factories/автоконфигурации.
    tasks.register<Sync>("appDist") {
        group = "build"
        description = "Собирает classpath-дистрибутив (jar'ы зависимостей + jar модуля) в build/dist/lib."
        from(configurations.getByName("runtimeClasspath"))
        from(tasks.named("jar"))
        into(layout.buildDirectory.dir("dist/lib"))
    }

    // appDist — основной артефакт приложения (Docker/запуск), вешаем на стандартный lifecycle.
    tasks.named("assemble") {
        dependsOn("appDist")
    }
}
