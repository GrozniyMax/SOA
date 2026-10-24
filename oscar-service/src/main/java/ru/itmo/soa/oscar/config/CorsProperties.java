package ru.itmo.soa.oscar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Настройки CORS (префикс {@code cors}).
 *
 * <p>Была record (конструкторная привязка — только Boot 2.6+); в Boot 2.3 привязка
 * идёт через JavaBean, поэтому класс мутабельный с дефолтом «пустой список».</p>
 */
@Data
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();
}
