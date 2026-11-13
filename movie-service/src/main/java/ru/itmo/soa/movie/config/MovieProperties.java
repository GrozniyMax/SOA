package ru.itmo.soa.movie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;

@Data
@ConfigurationProperties(prefix = "soa.movie")
public class MovieProperties {

    private int defaultPageSize = 20;

    private int maxPageSize = 100;

    private int taglineMaxLength = 181;

    @PostConstruct
    void applyFallbacks() {
        if (defaultPageSize <= 0) defaultPageSize = 20;
        if (maxPageSize <= 0) maxPageSize = 100;
        if (taglineMaxLength <= 0) taglineMaxLength = 181;
    }
}
