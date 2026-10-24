package ru.itmo.soa.oscar.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.itmo.soa.movie.client.MoviesApi;
import ru.itmo.soa.oscar.config.MovieFeignConfiguration;

/**
 * Spring-обёртка над сгенерированным из OpenAPI-спецификации movie-service
 * Feign-интерфейсом {@link MoviesApi}. Базовый URL задаётся настройкой
 * {@code movie.service.url} (значение приходит из config-repo; env MOVIE_SERVICE_URL).
 * Дефолт в placeholder нужен для локального запуска без Config Server —
 * локальный application.yml намеренно не задаёт свойство, иначе он перекрыл бы
 * remote-конфиг (у remote PropertySource приоритет ниже локального).
 */
@FeignClient(
        name = "movie-service",
        url = "${movie.service.url:http://localhost:8080}",
        configuration = MovieFeignConfiguration.class)
public interface MovieServiceFeignClient extends MoviesApi {
}
