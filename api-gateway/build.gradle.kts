// api-gateway: Zuul Proxy + Eureka client на общем стеке Boot 2.3 / Hoxton / Java 11.
// Реальный Netflix Zuul/Ribbon существует только в Hoxton.

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-zuul")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
}

