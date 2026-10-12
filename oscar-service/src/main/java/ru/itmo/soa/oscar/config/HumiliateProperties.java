package ru.itmo.soa.oscar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки операции отзыва Оскаров у режиссёров (префикс {@code movie.humiliate}).
 */
@ConfigurationProperties(prefix = "movie.humiliate")
public record HumiliateProperties(int batchSize) {
}
