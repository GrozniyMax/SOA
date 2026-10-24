// eureka-server: Netflix Eureka Service Discovery (Boot 3.5, общий стек от корня).

plugins {
    id("org.springframework.boot") version "3.5.3"
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
    }
}
