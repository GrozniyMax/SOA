package ru.itmo.soa.oscar.service;

import org.junit.jupiter.api.Test;
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
import ru.itmo.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

class OscarServiceImplTest {

    private final OscarDao dao = mock(OscarDao.class);
    private final MoviesApi moviesApi = mock(MoviesApi.class);
    private final OscarServiceImpl service =
            new OscarServiceImpl(dao, moviesApi, new HumiliateProperties(2));

    // ---- Вспомогательные конструкторы сгенерированных DTO ----

    private static Movie movie(int id, String director) {
        Person person = new Person();
        person.setName(director);
        // id у сгенерированного Movie выставляется только через @JsonCreator-конструктор
        Movie movie = new Movie(id, java.time.LocalDate.EPOCH);
        movie.setDirector(person);
        return movie;
    }

    private static MoviePage page(List<Movie> items, long total) {
        MoviePage result = new MoviePage();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(0);
        result.setSize(items.size());
        return result;
    }

    private static DirectorsHumiliationResult humiliation(Object... revokedByDirector) {
        List<DirectorsHumiliation> results = new ArrayList<>();
        for (int i = 0; i < revokedByDirector.length; i += 2) {
            DirectorsHumiliation entry = new DirectorsHumiliation();
            entry.setDirector((String) revokedByDirector[i]);
            entry.setRevokedOscars((Integer) revokedByDirector[i + 1]);
            results.add(entry);
        }
        DirectorsHumiliationResult result = new DirectorsHumiliationResult();
        result.setResults(results);
        return result;
    }

    private static List<String> directorsOf(DirectorsHumiliateRequest request) {
        return request.getDirectors();
    }

    // ---- getLosers ----

    @Test
    void getLosersPassesAwardedMovieIdsToDao() {
        when(moviesApi.searchMovies(any())).thenReturn(page(
                List.of(movie(3, "A"), movie(9, "B")), 2));
        PageResult<Operator> expected = new PageResult<>(List.of(new Operator(1L, "Deakins")), 1, 0, 10);
        when(dao.findLosers(anyCollection(), anyInt(), anyInt())).thenReturn(expected);

        PageResult<Operator> result = service.getLosers(0, 10);

        assertThat(result).isSameAs(expected);
        var request = verifySearchRequest();
        assertThat(request.getFilters()).hasSize(1);
        assertThat(request.getFilters().get(0).getPath()).isEqualTo(MovieFieldPath.OSCARS_COUNT);
        assertThat(request.getFilters().get(0).getValue()).isEqualTo("0");
        assertThat(request.getFilters().get(0).getComparator()).isEqualTo(Comparator.GT);
    }

    @Test
    void getLosersWalksAllPages() {
        List<Movie> firstHundred = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            firstHundred.add(movie(i, "A"));
        }
        when(moviesApi.searchMovies(any()))
                .thenReturn(page(firstHundred, 102))
                .thenReturn(page(List.of(movie(101, "B"), movie(102, "C")), 102));

        service.getLosers(0, 10);

        var pages = forClass(MovieSearchRequest.class);
        verify(moviesApi, times(2)).searchMovies(pages.capture());
        assertThat(pages.getAllValues().get(0).getPage()).isZero();
        assertThat(pages.getAllValues().get(1).getPage()).isEqualTo(1);
    }

    @Test
    void getLosersPropagatesMovieServiceErrors() {
        when(moviesApi.searchMovies(any())).thenThrow(new MovieServiceUnavailableException("down"));

        assertThatThrownBy(() -> service.getLosers(0, 10))
                .isInstanceOf(MovieServiceUnavailableException.class);
        verify(dao, never()).findLosers(anyCollection(), anyInt(), anyInt());
    }

    // ---- humiliateDirectorsByGenre ----

    @Test
    void humiliateWithNoDirectorsRevokesNothing() {
        when(moviesApi.searchMovies(any())).thenReturn(page(List.of(), 0));

        HumiliationResult result = service.humiliateDirectorsByGenre(MovieGenre.FANTASY);

        assertThat(result).isEqualTo(new HumiliationResult(MovieGenre.FANTASY, 0, 0));
        verify(moviesApi, never()).humiliateDirectors(any());
        var request = verifySearchRequest();
        assertThat(request.getFilters().get(0).getPath()).isEqualTo(MovieFieldPath.GENRE);
        assertThat(request.getFilters().get(0).getValue()).isEqualTo("FANTASY");
        assertThat(request.getFilters().get(0).getComparator()).isEqualTo(Comparator.EQ);
    }

    @Test
    void humiliateSplitsDirectorsIntoChunksByConfiguredBatchSize() {
        // Размер пачки — 2 (конфигурация HumiliateProperties), режиссёров — 3:
        // ожидается два запроса humiliateDirectors с разбиением [A, B] и [C].
        when(moviesApi.searchMovies(any())).thenReturn(page(List.of(
                movie(1, "Nolan"), movie(2, "Villeneuve"), movie(3, "Cameron")), 3));
        when(moviesApi.humiliateDirectors(any()))
                .thenReturn(humiliation("Nolan", 2, "Villeneuve", 0))
                .thenReturn(humiliation("Cameron", 3));

        HumiliationResult result = service.humiliateDirectorsByGenre(MovieGenre.ACTION);

        assertThat(result).isEqualTo(new HumiliationResult(MovieGenre.ACTION, 2, 5));
        var chunks = forClass(DirectorsHumiliateRequest.class);
        verify(moviesApi, times(2)).humiliateDirectors(chunks.capture());
        assertThat(chunks.getAllValues()).hasSize(2);
        assertThat(directorsOf(chunks.getAllValues().get(0))).containsExactly("Nolan", "Villeneuve");
        assertThat(directorsOf(chunks.getAllValues().get(1))).containsExactly("Cameron");
    }

    @Test
    void humiliateCountsOnlyDirectorsWithRevokedOscars() {
        when(moviesApi.searchMovies(any())).thenReturn(page(List.of(
                movie(1, "Nolan"), movie(2, "Nobody")), 2));
        when(moviesApi.humiliateDirectors(any()))
                .thenReturn(humiliation("Nolan", 4, "Nobody", 0));

        HumiliationResult result = service.humiliateDirectorsByGenre(MovieGenre.DRAMA);

        assertThat(result).isEqualTo(new HumiliationResult(MovieGenre.DRAMA, 1, 4));
    }

    @Test
    void humiliatePropagatesMovieServiceErrors() {
        when(moviesApi.searchMovies(any()))
                .thenThrow(new MovieServiceUnavailableException("movie-service down"));

        assertThatThrownBy(() -> service.humiliateDirectorsByGenre(MovieGenre.DRAMA))
                .isInstanceOf(MovieServiceUnavailableException.class)
                .hasMessage("movie-service down");
        verify(moviesApi, never()).humiliateDirectors(any());
    }

    private MovieSearchRequest verifySearchRequest() {
        var captor = forClass(MovieSearchRequest.class);
        verify(moviesApi).searchMovies(captor.capture());
        MovieSearchRequest request = captor.getValue();
        assertThat(request.getSort()).isEqualTo("id:asc");
        assertThat(request.getSize()).isEqualTo(100);
        return request;
    }
}
