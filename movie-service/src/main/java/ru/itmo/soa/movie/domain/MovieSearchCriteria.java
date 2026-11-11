package ru.itmo.soa.movie.domain;

import lombok.Value;

import java.util.List;

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
