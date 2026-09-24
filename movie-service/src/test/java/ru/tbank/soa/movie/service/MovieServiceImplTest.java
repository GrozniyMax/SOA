package ru.tbank.soa.movie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.soa.movie.config.MovieProperties;
import ru.tbank.soa.movie.domain.Coordinates;
import ru.tbank.soa.movie.domain.EyeColor;
import ru.tbank.soa.movie.domain.HairColor;
import ru.tbank.soa.movie.domain.Movie;
import ru.tbank.soa.movie.domain.MovieGenre;
import ru.tbank.soa.movie.domain.MovieSearchCriteria;
import ru.tbank.soa.movie.domain.PageResult;
import ru.tbank.soa.movie.domain.Person;
import ru.tbank.soa.movie.repository.dao.MovieDao;
import ru.tbank.soa.movie.service.exception.InvalidMovieDataException;
import ru.tbank.soa.movie.service.exception.MovieNotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

    @Mock
    private MovieDao dao;

    private MovieServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MovieServiceImpl(dao, new MovieProperties(20, 100, 181));
    }

    private static Movie movie() {
        return new Movie(
                0L, "Inception", new Coordinates(1, 2.5f), LocalDate.of(2020, 1, 1),
                2, 100.0f, "dream", MovieGenre.ACTION,
                new Person("Nolan", LocalDate.of(1970, 5, 5), EyeColor.BLUE, HairColor.BROWN));
    }

    @Test
    void addMovieForcesIdZeroAndGeneratesCreationDate() {
        when(dao.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        Movie result = service.addMovie(movie());

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(dao).save(captor.capture());
        Movie saved = captor.getValue();
        assertThat(saved.id()).isZero();
        assertThat(saved.creationDate()).isEqualTo(LocalDate.now());
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void addMovieRejectsBlankName() {
        Movie bad = new Movie(0L, "  ", new Coordinates(1, 1f), LocalDate.now(),
                1, 1f, "t", MovieGenre.ACTION, new Person("D", null, null, null));

        assertThatThrownBy(() -> service.addMovie(bad))
                .isInstanceOf(InvalidMovieDataException.class);
        verify(dao, never()).save(any());
    }

    @Test
    void addMovieRejectsTaglineLongerThanConfiguredMax() {
        Movie bad = new Movie(0L, "Name", new Coordinates(1, 1f), LocalDate.now(),
                1, 1f, "x".repeat(182), MovieGenre.ACTION, new Person("D", null, null, null));

        assertThatThrownBy(() -> service.addMovie(bad))
                .isInstanceOf(InvalidMovieDataException.class);
    }

    @Test
    void getMovieByIdReturnsMovieOrThrows() {
        Movie m = movie();
        when(dao.findById(1L)).thenReturn(Optional.of(m));

        assertThat(service.getMovieById(1L)).isEqualTo(m);

        when(dao.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMovieById(99L))
                .isInstanceOf(MovieNotFoundException.class);
    }

    @Test
    void updateMovieKeepsExistingIdAndCreationDate() {
        Movie existing = new Movie(7L, "Old", new Coordinates(1, 1f), LocalDate.of(2010, 6, 6),
                1, 1f, "t", MovieGenre.DRAMA, new Person("Dir", null, null, null));
        when(dao.findById(7L)).thenReturn(Optional.of(existing));
        when(dao.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        Movie updated = service.updateMovie(7L, movie());

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(dao).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(7L);
        assertThat(captor.getValue().creationDate()).isEqualTo(LocalDate.of(2010, 6, 6));
        assertThat(captor.getValue().name()).isEqualTo("Inception");
        assertThat(updated.id()).isEqualTo(7L);
    }

    @Test
    void updateMovieThrowsWhenAbsent() {
        when(dao.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMovie(1L, movie()))
                .isInstanceOf(MovieNotFoundException.class);
    }

    @Test
    void deleteMovieThrowsWhenAbsentAndDeletesWhenPresent() {
        when(dao.existsById(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.deleteMovie(1L)).isInstanceOf(MovieNotFoundException.class);

        when(dao.existsById(2L)).thenReturn(true);
        service.deleteMovie(2L);
        verify(dao).deleteById(2L);
    }

    @Test
    void getMovieWithMaxTaglineThrowsWhenNone() {
        when(dao.findWithMaxTagline()).thenReturn(Optional.empty());

        assertThatThrownBy(service::getMovieWithMaxTagline)
                .isInstanceOf(MovieNotFoundException.class);
    }

    @Test
    void delegatesSearchDirectorAndGenres() {
        MovieSearchCriteria criteria = new MovieSearchCriteria(List.of(), "name:asc", 0, 20);
        PageResult<Movie> page = new PageResult<>(List.of(movie()), 1, 0, 20);
        when(dao.search(criteria)).thenReturn(page);
        when(dao.findByDirectorGreaterThan("Nolan", 0, 20)).thenReturn(page);
        PageResult<MovieGenre> genres = new PageResult<>(List.of(MovieGenre.ACTION), 1, 0, 20);
        when(dao.findUniqueGenres(0, 20)).thenReturn(genres);

        assertThat(service.searchMovies(criteria)).isSameAs(page);
        assertThat(service.getByDirectorGreaterThan("Nolan", 0, 20)).isSameAs(page);
        assertThat(service.getUniqueGenres(0, 20)).isSameAs(genres);
    }
}