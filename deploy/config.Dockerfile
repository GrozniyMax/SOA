FROM eclipse-temurin:21-jre

# В базовом образе нет curl, а bash собран без /dev/tcp — healthcheck требует curl.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY config-server/build/libs/config-server-0.0.1-SNAPSHOT.jar /app/config-server.jar

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app/config-server.jar"]
