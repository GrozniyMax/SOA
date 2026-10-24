FROM eclipse-temurin:21-jre

# В базовом образе нет curl, а bash собран без /dev/tcp — healthcheck требует curl.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY eureka-server/build/libs/eureka-server-0.0.1-SNAPSHOT.jar /app/eureka-server.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/eureka-server.jar"]
