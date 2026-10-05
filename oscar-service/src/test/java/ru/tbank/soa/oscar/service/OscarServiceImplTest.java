package ru.tbank.soa.oscar.service;

import org.junit.jupiter.api.Test;
import ru.tbank.soa.oscar.client.MovieServiceClient;
import ru.tbank.soa.oscar.domain.HumiliationResult;
import ru.tbank.soa.oscar.domain.Movie;
import ru.tbank.soa.oscar.domain.MovieGenre;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;
import ru.tbank.soa.oscar.repository.dao.OscarDao;
import ru.tbank.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OscarServiceImplTest {

    private final OscarDao dao = mock(OscarDao.class);
    private final MovieServiceClient client = mock(MovieServiceClient.class);
    private final OscarServiceImpl service = new OscarServiceImpl(dao, client);

    @Test
    void getLosersPassesAwardedMovieIdsToDao() {
        PageResult<Operator> expected = new PageResult<>(List.of(new Operator(1L, "Deakins")), 1, 0, 10);
        when(client.findAwardedMovieIds()).thenReturn(Set.of(3L, 4L));
        when(dao.findLosers(Set.of(3L, 4L), 0, 10)).thenReturn(expected);

        PageResult<Operator> result = service.getLosers(0, 10);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getLosersPropagatesMovieServiceErrors() {
        when(client.findAwardedMovieIds()).thenThrow(new MovieServiceUnavailableException("down"));

        assertThatThrownBy(() -> service.getLosers(0, 10))
                .isInstanceOf(MovieServiceUnavailableException.class);
        verify(dao, never()).findLosers(anyCollection(), anyInt(), anyInt());
    }

    @Test
    void humiliateWithNoDirectorsRevokesNothing() {
        when(client.findDirectorsByGenre(MovieGenre.FANTASY)).thenReturn(Set.of());

        HumiliationResult result = service.humiliateDirectorsByGenre(MovieGenre.FANTASY);

        assertThat(result).isEqualTo(new HumiliationResult(MovieGenre.FANTASY, 0, 0));
        verify(client, never()).revokeOscars(anyLong());
    }

    @Test
    void humiliateRevokesAllAwardedMoviesOfDirectors() {
        when(client.findDirectorsByGenre(MovieGenre.ACTION)).thenReturn(Set.of("Nolan", "Villeneuve", "Nobody"));
        when(client.findAwardedMoviesByDirector("Nolan")).thenReturn(List.of(new Movie(1L, 2), new Movie(2L, 3)));
        when(client.findAwardedMoviesByDirector("Villeneuve")).thenReturn(List.of(new Movie(5L, 1)));
        when(client.findAwardedMoviesByDirector("Nobody")).thenReturn(List.of());

        HumiliationResult result = service.humiliateDirectorsByGenre(MovieGenre.ACTION);

        assertThat(result).isEqualTo(new HumiliationResult(MovieGenre.ACTION, 2, 6));
        verify(client).revokeOscars(1L);
        verify(client).revokeOscars(2L);
        verify(client).revokeOscars(5L);
    }

    @Test
    void humiliatePropagatesMovieServiceErrors() {
        when(client.findDirectorsByGenre(MovieGenre.DRAMA))
                .thenThrow(new MovieServiceUnavailableException("movie-service down"));

        assertThatThrownBy(() -> service.humiliateDirectorsByGenre(MovieGenre.DRAMA))
                .isInstanceOf(MovieServiceUnavailableException.class)
                .hasMessage("movie-service down");
        verify(client, never()).revokeOscars(anyLong());
    }
}
