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
}
