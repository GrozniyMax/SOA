# movie-service: Spring Boot 2.3 / Java 11, classpath-дистрибутив (lib/),
# внутренний plain HTTP :8080. Снаружи доступен только через api-gateway —
# порт наружу не публикуется.
FROM eclipse-temurin:11-jre

WORKDIR /app
COPY movie-service/build/dist/lib /app/lib

EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/app/lib/*", "ru.itmo.soa.movie.MovieServiceApplication"]
