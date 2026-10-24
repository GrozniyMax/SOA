FROM eclipse-temurin:21-jre

WORKDIR /app
COPY config-server/build/libs/config-server-0.0.1-SNAPSHOT.jar /app/config-server.jar

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app/config-server.jar"]
