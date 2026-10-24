package ru.itmo.soa.movie.domain;

import lombok.Value;

import java.util.List;

/**
 * Критерии динамического поиска фильмов (POST /movies/search).
 *
 * <p>Была record с компактным конструктором; переписана в immutable-класс (Java 11)
 * в стиле Lombok {@code @Value}. Нормализация списка фильтров сохранена в конструкторе.</p>
 */
@Value
public class MovieSearchCriteria {

    List<Filter> filters;

    String sort;

    int page;

    int size;

    public MovieSearchCriteria(List<Filter> filters, String sort, int page, int size) {
        this.filters = filters == null ? List.of() : List.copyOf(filters);
        this.sort = sort;
        this.page = page;
        this.size = size;
    }
}
