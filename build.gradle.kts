// Общий (корневой) build.gradle.kts: единый стек Spring Boot 2.3
plugins {
    java
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "ru.itmo.soa"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:2.3.12.RELEASE")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:Hoxton.SR12")
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

    configurations.all {
        resolutionStrategy {
            force("com.google.code.gson:gson:2.8.6")
        }
    }

    tasks.register<Sync>("appDist") {
        group = "build"
        description = "Собирает classpath-дистрибутив (jar'ы зависимостей + jar модуля) в build/dist/lib."
        from(configurations.getByName("runtimeClasspath"))
        from(tasks.named("jar"))
        into(layout.buildDirectory.dir("dist/lib"))
    }

    tasks.named("assemble") {
        dependsOn("appDist")
    }
}
