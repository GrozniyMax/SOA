// config-server: Spring Cloud Config Server на общем стеке Boot 2.3 / Hoxton / Java 11.

dependencies {
    implementation("org.springframework.cloud:spring-cloud-config-server")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:Hoxton.SR12")
    }
}
