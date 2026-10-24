# oscar-service: Spring Boot 2.3 / Java 11, classpath-дистрибутив (lib/),
# plain HTTP на ${OSCAR_PORT} (8081 у oscar-1, 8082 у oscar-2).
# Порт наружу не публикуется — трафик идёт через haproxy.
# Межсервисные Feign-вызовы movie-service идут через gateway (TLS) —
# сертификат в /certs (см. docker-compose: ./certs:/certs:ro).
FROM eclipse-temurin:11-jre

WORKDIR /app
COPY oscar-service/build/dist/lib /app/lib

# Один образ на два инстанса: порт задаётся через OSCAR_PORT.
EXPOSE 8081 8082
ENTRYPOINT ["java", "-cp", "/app/lib/*", "ru.itmo.soa.oscar.OscarServiceApplication"]
