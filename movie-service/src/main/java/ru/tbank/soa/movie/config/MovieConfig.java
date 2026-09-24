package ru.tbank.soa.movie.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация movie-service. Регистрирует {@link MovieProperties}.
 */
@Configuration
@EnableConfigurationProperties(MovieProperties.class)
public class MovieConfig {
}