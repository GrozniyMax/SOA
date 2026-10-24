plugins {
    // Автозагрузка JDK для toolchain (нужен Java 11 для legacy api-gateway).
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "soa"

include("movie-service")
include("oscar-service")
include("config-server")
include("eureka-server")
include("api-gateway")