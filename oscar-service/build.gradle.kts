// oscar-service: standalone JAR с embedded Tomcat; вызывает REST API movie-service.
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    // WebClient для вызова API первого сервиса (movie-service).
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}