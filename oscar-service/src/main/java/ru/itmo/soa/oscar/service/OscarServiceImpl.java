package ru.itmo.soa.oscar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.soa.movie.client.MoviesApi;
import ru.itmo.soa.movie.dto.Comparator;
import ru.itmo.soa.movie.dto.DirectorsHumiliateRequest;
import ru.itmo.soa.movie.dto.DirectorsHumiliation;
import ru.itmo.soa.movie.dto.DirectorsHumiliationResult;
import ru.itmo.soa.movie.dto.Movie;
import ru.itmo.soa.movie.dto.MovieFieldPath;
import ru.itmo.soa.movie.dto.MovieFilter;
import ru.itmo.soa.movie.dto.MoviePage;
import ru.itmo.soa.movie.dto.MovieSearchRequest;
import ru.itmo.soa.movie.dto.Person;
import ru.itmo.soa.oscar.config.HumiliateProperties;
import ru.itmo.soa.oscar.domain.HumiliationResult;
import ru.itmo.soa.oscar.domain.MovieGenre;
import ru.itmo.soa.oscar.domain.Operator;
import ru.itmo.soa.oscar.domain.PageResult;
import ru.itmo.soa.oscar.repository.dao.OscarDao;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

@Service
@RequiredArgsConstructor
public class OscarServiceImpl implements OscarService {

    /** Размер страницы при постраничной выборке фильмов из movie-service. */
    private static final int SEARCH_PAGE_SIZE = 100;
    /** Стабильный порядок выборки для детерминированного разбиения на пачки. */
    private static final String SEARCH_SORT = "id:asc";

    private final OscarDao dao;
    private final MoviesApi moviesApi;
    private final HumiliateProperties humiliateProperties;

    @Override
    public PageResult<Operator> getLosers(int page, int size) {
        Set<Long> awardedFilmIds = searchAll(List.of(
                        new MovieFilter().path(MovieFieldPath.OSCARS_COUNT).value("0").comparator(Comparator.GT)))
                .stream()
                .map(Movie::getId)
                .map(Long::valueOf)
                .collect(toSet());
        return dao.findLosers(awardedFilmIds, page, size);
    }

    @Override
    public HumiliationResult humiliateDirectorsByGenre(MovieGenre genre) {
        Set<String> directors = searchAll(List.of(
                        new MovieFilter().path(MovieFieldPath.GENRE).value(genre.name()).comparator(Comparator.EQ)))
                .stream()
                .map(Movie::getDirector)
                .filter(d -> d != null && d.getName() != null)
                .map(Person::getName)
                .collect(toCollection(LinkedHashSet::new));
        if (directors.isEmpty()) {
            return new HumiliationResult(genre, 0, 0);
        }

        int batchSize = Math.max(1, humiliateProperties.batchSize());
        List<String> ordered = new ArrayList<>(directors);
        int affectedDirectors = 0;
        int revokedOscars = 0;
        for (int from = 0; from < ordered.size(); from += batchSize) {
            List<String> chunk = ordered.subList(from, Math.min(from + batchSize, ordered.size()));
            DirectorsHumiliationResult result = moviesApi.humiliateDirectors(
                    new DirectorsHumiliateRequest().directors(new ArrayList<>(chunk)));
            for (DirectorsHumiliation humiliation : result.getResults()) {
                int revoked = humiliation.getRevokedOscars() == null ? 0 : humiliation.getRevokedOscars();
                revokedOscars += revoked;
                if (revoked > 0) {
                    affectedDirectors++;
                }
            }
        }

        return new HumiliationResult(genre, affectedDirectors, revokedOscars);
    }

    /**
     * Обходит все страницы выборки movie-service, пока не прочитаны все элементы
     * (условие остановки, как раньше: пока {@code (page + 1) * size < total}).
     */
    private List<Movie> searchAll(List<MovieFilter> filters) {
        List<Movie> items = new ArrayList<>();
        int page = 0;
        while (true) {
            MoviePage response = moviesApi.searchMovies(new MovieSearchRequest()
                    .filters(filters)
                    .sort(SEARCH_SORT)
                    .page(page)
                    .size(SEARCH_PAGE_SIZE));
            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                return items;
            }
            items.addAll(response.getItems());
            if ((long) (page + 1) * SEARCH_PAGE_SIZE >= response.getTotal()) {
                return items;
            }
            page++;
        }
    }
}
