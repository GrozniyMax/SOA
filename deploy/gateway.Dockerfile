# api-gateway: classpath-дистрибутив (fat-jar ломает Zuul, см. build.gradle.kts).
FROM eclipse-temurin:11-jre

WORKDIR /app
COPY api-gateway/build/dist/lib /app/lib

EXPOSE 8443
ENTRYPOINT ["java", "-cp", "/app/lib/*", "ru.itmo.soa.gateway.ApiGatewayApplication"]
