# movie-service: исполняемый boot-jar (Boot 3.5 / Java 21), внутренний plain HTTP :8080.
# Снаружи доступен только через api-gateway — порт наружу не публикуется.
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY movie-service/build/libs/movie-service.jar /app/movie-service.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/movie-service.jar"]
