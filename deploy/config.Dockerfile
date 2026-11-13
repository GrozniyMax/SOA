# config-server: Spring Boot 2.3 / Java 11, classpath-дистрибутив (lib/).
FROM eclipse-temurin:11-jre

# В базовом образе нет curl, а bash собран без /dev/tcp — healthcheck требует curl.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY config-server/build/dist/lib /app/lib

EXPOSE 8888
ENTRYPOINT ["java", "-cp", "/app/lib/*", "ru.itmo.soa.config.ConfigServerApplication"]
