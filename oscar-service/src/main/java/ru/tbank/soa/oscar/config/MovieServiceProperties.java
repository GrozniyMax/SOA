package ru.tbank.soa.oscar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки доступа к movie-service (префикс {@code movie.service}).
 */
@ConfigurationProperties(prefix = "movie.service")
public record MovieServiceProperties(String url) {
}