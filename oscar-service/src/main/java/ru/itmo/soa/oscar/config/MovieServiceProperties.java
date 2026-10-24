package ru.itmo.soa.oscar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки доступа к movie-service (префикс {@code movie.service}).
 *
 * <p>Была record; в Boot 2.3 привязка JavaBean, поэтому класс мутабельный.</p>
 */
@Data
@ConfigurationProperties(prefix = "movie.service")
public class MovieServiceProperties {

    private String url = "http://localhost:8080";

    private String truststore;

    private String truststorePassword;
}
