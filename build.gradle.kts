// Общий (корневой) build.gradle.kts: единый стек Spring Boot 2.3 / Spring Cloud Hoxton / Java 11.
plugins {
    java
    id("io.spring.dependency-management") version "1.1.7" apply false
    // Плагин Spring Boot НЕ применяем нигде: gradle-плагин Boot 2.3.x несовместим
    // с Gradle 8.14 (LazyPublishArtifact NoSuchMethodError). Версии Boot пинятся
    // транзитивно через BOM spring-cloud-dependencies:Hoxton.SR12
    // (импортирует spring-boot-dependencies:2.3.12.RELEASE).
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
            // Плагин dependency-management не раскрывает цепочку импортов BOM'ов,
            // поэтому spring-boot-dependencies (Boot 2.3.12) импортируем явно.
            mavenBom("org.springframework.boot:spring-boot-dependencies:2.3.12.RELEASE")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:Hoxton.SR12")
            // Hoxton пинит feign 10.12 через вложенный feign-bom (цепочка импортов,
            // которую плагин не раскрывает) — импортируем явно, иначе feign уедет в 13.x.
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
    // Classpath-дистрибутив вместо fat-jar: при склейке нескольких jar'ов
    // META-INF/spring.factories мержится по правилу Properties (последний ключ
    // побеждает), и автоконфигурации Zuul/Eureka молча теряются. Поэтому все jar'ы
    // кладём рядом в build/dist/lib и запускаем через java -cp "lib/*" MainClass.
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
