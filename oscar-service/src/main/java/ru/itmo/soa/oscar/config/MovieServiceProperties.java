package ru.itmo.soa.oscar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "movie.service")
public class MovieServiceProperties {

    private String truststore;

    private String truststorePassword;
}
