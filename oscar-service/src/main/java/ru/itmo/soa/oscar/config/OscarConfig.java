package ru.itmo.soa.oscar.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Oscar Service: включает привязку {@link MovieServiceProperties}
 * и {@link HumiliateProperties}.
 */
@Configuration
@EnableConfigurationProperties({MovieServiceProperties.class, HumiliateProperties.class})
public class OscarConfig {
}