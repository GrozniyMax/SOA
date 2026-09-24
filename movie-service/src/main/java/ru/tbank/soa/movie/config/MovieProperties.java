package ru.tbank.soa.movie.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки movie-service (префикс {@code soa.movie}).
 */
@ConfigurationProperties(prefix = "soa.movie")
public record MovieProperties(
        int defaultPageSize,
        int maxPageSize,
        int taglineMaxLength) {

    public MovieProperties {
        if (defaultPageSize <= 0) {
            defaultPageSize = 20;
        }
        if (maxPageSize <= 0) {
            maxPageSize = 100;
        }
        if (taglineMaxLength <= 0) {
            taglineMaxLength = 181;
        }
    }
}