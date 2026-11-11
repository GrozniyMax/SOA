# movie-service: classpath-дистрибутив, внутренний HTTP :8080 (наружу не публикуется).
FROM eclipse-temurin:11-jre

WORKDIR /app
COPY movie-service/build/dist/lib /app/lib

EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/app/lib/*", "ru.itmo.soa.movie.MovieServiceApplication"]
