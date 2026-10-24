FROM eclipse-temurin:21-jre

WORKDIR /app
COPY eureka-server/build/libs/eureka-server-0.0.1-SNAPSHOT.jar /app/eureka-server.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/eureka-server.jar"]
