// api-gateway: LEGACY island — Spring Boot 2.3.12.RELEASE + Spring Cloud Hoxton.SR12 + Java 11.
// Реальный Zuul Proxy + Ribbon существуют только в Hoxton, поэтому этот модуль
// намеренно изолирован от корневого стека Boot 3.5/Java 21.
//
// ВАЖНО: gradle-плагин Spring Boot 2.3.12 несовместим с Gradle 8.14
// (NoSuchMethodError: LazyPublishArtifact.<init>(Provider)), поэтому плагин НЕ применяется.
// Версии Boot 2.3/Hoxton пинятся через BOM spring-cloud-dependencies:Hoxton.SR12
// (он транзитивно импортирует spring-boot-dependencies:2.3.12.RELEASE).

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-zuul")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:Hoxton.SR12")
    }
}

// Hoxton пинит gson 2.1, но eureka-client требует GsonBuilder.setLenient (появился в 2.2+).
configurations.all {
    resolutionStrategy {
        force("com.google.code.gson:gson:2.8.6")
    }
}

// Раскладка classpath-дистрибутива вместо fat-jar: склейка META-INF/spring.factories
// из нескольких jar'ов ломает автоконфигурацию Zuul/Eureka (java.util.Properties
// оставляет последнее значение при дублирующемся ключе, конфигурации теряются),
// поэтому кладём все jar'ы рядом и запускаем через java -cp "lib/*".
tasks.register<Sync>("gatewayDist") {
    group = "build"
    from(configurations.runtimeClasspath.get())
    from(tasks.jar)
    into(layout.buildDirectory.dir("dist/lib"))
}

tasks.named("build") {
    dependsOn("gatewayDist")
}
