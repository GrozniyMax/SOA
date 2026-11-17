# eureka-dns: Java 11 DNS-прокси, публикующий Eureka-инстансы как SRV/A записи.
FROM eclipse-temurin:11-jre

WORKDIR /app
COPY eureka-dns/build/dist/lib /app/lib

# TCP и UDP DNS-порт.
EXPOSE 8053/tcp 8053/udp
ENTRYPOINT ["java", "-cp", "/app/lib/*", "ru.itmo.soa.eurekadns.EurekaDnsApplication"]