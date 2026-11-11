# oscar-service: classpath-дистрибутив, HTTP на ${OSCAR_PORT}; наружу не публикуется, Feign через gateway (TLS).
FROM eclipse-temurin:11-jre

WORKDIR /app
COPY oscar-service/build/dist/lib /app/lib

# Один образ на два инстанса: порт задаётся через OSCAR_PORT.
EXPOSE 8081 8082
ENTRYPOINT ["java", "-cp", "/app/lib/*", "ru.itmo.soa.oscar.OscarServiceApplication"]
