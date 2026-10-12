package ru.itmo.soa.oscar.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.itmo.soa.movie.client.MoviesApi;
import ru.itmo.soa.oscar.config.MovieFeignConfiguration;

/**
 * Spring-обёртка над сгенерированным из OpenAPI-спецификации movie-service
 * Feign-интерфейсом {@link MoviesApi}. Базовый URL задаётся настройкой
 * {@code movie.service.url}. Используется вместо ручного WebClient-клиента
 * (переключение точек вызова — отдельный этап).
 */
@FeignClient(
        name = "movie-service",
        url = "${movie.service.url}",
        configuration = MovieFeignConfiguration.class)
public interface MovieServiceFeignClient extends MoviesApi {
}
