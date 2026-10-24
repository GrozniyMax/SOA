# api-gateway: LEGACY-остров — Spring Boot 2.3 / Spring Cloud Hoxton / Java 11.
# Дистрибутив — раскладка lib/ (classpath), а не fat-jar: склейка spring.factories
# ломает автоконфигурацию Zuul (см. комментарий в build.gradle.kts).
FROM eclipse-temurin:11-jre

WORKDIR /app
COPY api-gateway/build/dist/lib /app/lib

EXPOSE 8443
ENTRYPOINT ["java", "-cp", "/app/lib/*", "ru.itmo.soa.gateway.ApiGatewayApplication"]
