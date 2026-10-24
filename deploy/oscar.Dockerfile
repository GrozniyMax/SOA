# oscar-service: исполняемый boot-jar (Boot 3.5 / Java 21), plain HTTP на ${OSCAR_PORT}
# (8081 у oscar-1, 8082 у oscar-2). Порт наружу не публикуется — трафик идёт через haproxy.
# Межсервисные Feign-вызовы movie-service идут через gateway (TLS) — сертификат в /certs.
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY oscar-service/build/libs/oscar.jar /app/oscar.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/oscar.jar"]
