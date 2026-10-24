# api-gateway: LEGACY-остров — Spring Boot 2.3 / Spring Cloud Hoxton / Java 11.
# Shadow-jar собирается плагином com.github.johnrengelman.shadow (Main-Class задан в build.gradle.kts).
FROM eclipse-temurin:11-jre

WORKDIR /app
# Точное имя: в build/libs лежит ещё и api-gateway-*-plain.jar — glob его захватит.
COPY api-gateway/build/libs/api-gateway-0.0.1-SNAPSHOT.jar /app/api-gateway.jar

EXPOSE 8443
ENTRYPOINT ["java", "-jar", "/app/api-gateway.jar"]
