package ru.tbank.soa.oscar.service;

import org.junit.jupiter.api.Test;
import ru.tbank.soa.oscar.client.MovieServiceClient;
import ru.tbank.soa.oscar.domain.Award;
import ru.tbank.soa.oscar.domain.HumiliationResult;
import ru.tbank.soa.oscar.domain.MovieGenre;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;
import ru.tbank.soa.oscar.repository.dao.OscarDao;
import ru.tbank.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OscarServiceImplTest {

    private final OscarDao dao = mock(OscarDao.class);
    private final MovieServiceClient client = mock(MovieServiceClient.class);
    private final OscarServiceImpl service = new OscarServiceImpl(dao, client);

    @Test
    void getLosersDelegatesToDao() {
        PageResult<Operator> expected = new PageResult<>(List.of(new Operator(1L, "Deakins")), 1, 0, 10);
        when(dao.findLosers(0, 10)).thenReturn(expected);

        PageResult<Operator> result = service.getLosers(0, 10);

        assertThat(result).isSameAs(expected);
        verify(dao).findLosers(0, 10);
    }

    @Test
    void humiliateWithNoDirectorsReturnsZeroAndNoRevoke() {
        when(client.findDirectorsByGenre(MovieGenre.FANTASY)).thenReturn(Set.of());
        when(dao.findAwardsByDirectorIn(Set.of())).thenReturn(List.of());

        HumiliationResult result = service.humiliateDirectorsByGenre(MovieGenre.FANTASY);

        assertThat(result.genre()).isEqualTo(MovieGenre.FANTASY);
        assertThat(result.affectedDirectors()).isZero();
        assertThat(result.revokedOscars()).isZero();
        verify(dao, never()).revokeOscars(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void humiliateWithDirectorsRevokesPositiveAwards() {
        when(client.findDirectorsByGenre(MovieGenre.ACTION))
                .thenReturn(Set.of("Christopher Nolan", "Some Director"));

        when(dao.findAwardsByDirectorIn(Set.of("Christopher Nolan", "Some Director"))).thenReturn(List.of(
                new Award(1L, 1L, 2, MovieGenre.ACTION, "Christopher Nolan"),
                new Award(2L, 2L, 0, MovieGenre.DRAMA, "Christopher Nolan"),
                new Award(3L, 3L, 1, MovieGenre.ACTION, "Some Director")
        ));

        HumiliationResult result = service.humiliateDirectorsByGenre(MovieGenre.ACTION);

        assertThat(result.genre()).isEqualTo(MovieGenre.ACTION);
        // Затронуты режиссёры только у записей с oscarsCount > 0: Nolan и Some Director.
        assertThat(result.affectedDirectors()).isEqualTo(2);
        assertThat(result.revokedOscars()).isEqualTo(3);
        verify(dao).revokeOscars(List.of(1L, 3L));
    }

    @Test
    void humiliateOnlyCountsDistinctAffectedDirectors() {
        when(client.findDirectorsByGenre(MovieGenre.ACTION)).thenReturn(Set.of("Nolan"));
        when(dao.findAwardsByDirectorIn(Set.of("Nolan"))).thenReturn(List.of(
                new Award(1L, 1L, 2, MovieGenre.ACTION, "Nolan"),
                new Award(2L, 2L, 3, MovieGenre.ACTION, "Nolan")
        ));

        HumiliationResult result = service.humiliateDirectorsByGenre(MovieGenre.ACTION);

        assertThat(result.affectedDirectors()).isEqualTo(1);
        assertThat(result.revokedOscars()).isEqualTo(5);
        verify(dao).revokeOscars(List.of(1L, 2L));
    }

    @Test
    void humiliatePropagatesMovieServiceUnavailableException() {
        when(client.findDirectorsByGenre(MovieGenre.DRAMA))
                .thenThrow(new MovieServiceUnavailableException("movie-service down"));

        assertThatThrownBy(() -> service.humiliateDirectorsByGenre(MovieGenre.DRAMA))
                .isInstanceOf(MovieServiceUnavailableException.class)
                .hasMessage("movie-service down");

        verify(dao, never()).findAwardsByDirectorIn(org.mockito.ArgumentMatchers.anyCollection());
        verify(dao, never()).revokeOscars(org.mockito.ArgumentMatchers.anyList());
    }
}