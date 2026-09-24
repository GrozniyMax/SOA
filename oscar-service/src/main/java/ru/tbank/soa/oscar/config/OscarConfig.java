package ru.tbank.soa.oscar.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Oscar Service: включает привязку {@link MovieServiceProperties}.
 */
@Configuration
@EnableConfigurationProperties(MovieServiceProperties.class)
public class OscarConfig {
}