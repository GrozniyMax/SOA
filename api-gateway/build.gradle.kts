// api-gateway: LEGACY island — Spring Boot 2.3.12.RELEASE + Spring Cloud Hoxton.SR12 + Java 11.
// Реальный Zuul Proxy + Ribbon существуют только в Hoxton, поэтому этот модуль
// намеренно изолирован от корневого стека Boot 3.5/Java 21.
//
// ВАЖНО: gradle-плагин Spring Boot 2.3.12 несовместим с Gradle 8.14
// (NoSuchMethodError: LazyPublishArtifact.<init>(Provider)), поэтому плагин НЕ применяется.
// Версии Boot 2.3/Hoxton пинятся через BOM spring-cloud-dependencies:Hoxton.SR12
// (он транзитивно импортирует spring-boot-dependencies:2.3.12.RELEASE),
// а исполняемый jar собирается плагином Shadow с Main-Class = приложение.

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-zuul")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:Hoxton.SR12")
    }
}

// Транзитивно подтягиваются разные версии gson — для shadow-жира нужна одна.
configurations.all {
    resolutionStrategy {
        force("com.google.code.gson:gson:2.8.6")
    }
}

tasks {
    jar {
        // Классификатор нужен, чтобы plain jar не конфликтовал по имени
        // с classifier-less shadow-жиром (оба называются api-gateway.jar).
        archiveClassifier.set("plain")
    }

    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("api-gateway")
        mergeServiceFiles()
        // Несколько jar'ов несут свои META-INF/spring.factories (Boot, Zuul, Ribbon...) —
        // для автоконфигурации их нужно склеить.
        append("META-INF/spring.factories")
        append("META-INF/spring.handlers")
        append("META-INF/spring.schemas")
        manifest {
            attributes["Main-Class"] = "ru.itmo.soa.gateway.ApiGatewayApplication"
        }
    }

    build {
        dependsOn(shadowJar)
    }
}
