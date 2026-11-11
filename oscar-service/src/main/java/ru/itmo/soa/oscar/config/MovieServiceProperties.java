package ru.itmo.soa.oscar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "movie.service")
public class MovieServiceProperties {

    private String url = "http://localhost:8080";

    private String truststore;

    private String truststorePassword;
}
