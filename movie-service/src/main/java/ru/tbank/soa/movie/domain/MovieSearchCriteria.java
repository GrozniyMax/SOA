package ru.tbank.soa.movie.domain;

import java.util.List;

/**
 * Критерии динамического поиска фильмов (POST /movies/search).
 *
 * @param filters список фильтров
 * @param sort    строка сортировки, например "name:asc,oscarsCount:desc"
 * @param page    номер страницы (>= 0)
 * @param size    размер страницы (1..100)
 */
public record MovieSearchCriteria(List<Filter> filters, String sort, int page, int size) {

    /**
     * Компактный конструктор: пустой список фильтров по умолчанию.
     */
    public MovieSearchCriteria {
        filters = filters == null ? List.of() : List.copyOf(filters);
    }
}