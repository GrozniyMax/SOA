package ru.itmo.soa.movie.repository;

import org.junit.jupiter.api.Test;
import javax.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.soa.movie.domain.Coordinates;
import ru.itmo.soa.movie.domain.DirectorsHumiliation;
import ru.itmo.soa.movie.domain.EyeColor;
import ru.itmo.soa.movie.domain.Filter;
import ru.itmo.soa.movie.domain.FilterComparator;
import ru.itmo.soa.movie.domain.HairColor;
import ru.itmo.soa.movie.domain.Movie;
import ru.itmo.soa.movie.domain.MovieFieldPath;
import ru.itmo.soa.movie.domain.MovieGenre;
import ru.itmo.soa.movie.domain.MovieSearchCriteria;
import ru.itmo.soa.movie.domain.PageResult;
import ru.itmo.soa.movie.domain.Person;
import ru.itmo.soa.movie.repository.dao.JpaMovieDao;
import ru.itmo.soa.movie.repository.dao.MovieDao;
import ru.itmo.soa.movie.repository.entity.CoordinatesEntity;
import ru.itmo.soa.movie.repository.jpa.CoordinatesJpaRepository;
import ru.itmo.soa.movie.repository.jpa.PersonJpaRepository;
import ru.itmo.soa.movie.repository.mapper.MovieEntityMapperImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({MovieEntityMapperImpl.class, JpaMovieDao.class})
class JpaMovieDaoTest {

    @Autowired
    private MovieDao dao;

    @Autowired
    private CoordinatesJpaRepository coordinatesRepository;

    @Autowired
    private PersonJpaRepository personRepository;

    @Autowired
    private EntityManager entityManager;

    private static Movie movie(String name, String tagline, MovieGenre genre, String directorName) {
        return new Movie(
                0L,
                name,
                new Coordinates(1, 2.5f),
                LocalDate.of(2020, 1, 1),
                2,
                100.0f,
                tagline,
                genre,
                new Person(directorName, LocalDate.of(1970, 5, 5),
                        EyeColor.BLUE, HairColor.BROWN));
    }

    @Test
    void saveCreatesMovieWithGeneratedId() {
        Movie saved = dao.save(movie("Inception", "dream", MovieGenre.ACTION, "Nolan"));

        assertThat(saved.getId()).isPositive();
        assertThat(saved.getName()).isEqualTo("Inception");
        assertThat(saved.getCoordinates().getX()).isEqualTo(1);
        assertThat(saved.getCoordinates().getY()).isEqualTo(2.5f);
        assertThat(saved.getDirector().getName()).isEqualTo("Nolan");
        assertThat(dao.existsById(saved.getId())).isTrue();
    }

    @Test
    void saveWithExistingIdUpdates() {
        Movie created = dao.save(movie("Old", "tag", MovieGenre.DRAMA, "Director"));
        Movie update = new Movie(
                created.getId(),
                "New",
                new Coordinates(9, 9.9f),
                created.getCreationDate(),
                5,
                null,
                null,
                MovieGenre.FANTASY,
                new Person("New Director", null, null, null));

        Movie updated = dao.save(update);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getGenre()).isEqualTo(MovieGenre.FANTASY);
        assertThat(dao.findById(created.getId()).orElseThrow().getName()).isEqualTo("New");
    }

    @Test
    void findByIdAndDelete() {
        Movie saved = dao.save(movie("A", "t", MovieGenre.ACTION, "D"));

        assertThat(dao.findById(saved.getId())).isPresent();
        dao.deleteById(saved.getId());
        assertThat(dao.findById(saved.getId())).isEmpty();
    }

    @Test
    void findWithMaxTaglineReturnsLexicographicallyGreatest() {
        dao.save(movie("A", "aaa", MovieGenre.ACTION, "D1"));
        dao.save(movie("B", "zzz", MovieGenre.ACTION, "D2"));
        dao.save(movie("C", "mmm", MovieGenre.ACTION, "D3"));

        Optional<Movie> max = dao.findWithMaxTagline();

        assertThat(max).isPresent();
        assertThat(max.get().getTagline()).isEqualTo("zzz");
    }

    @Test
    void searchSupportsEqGtAndSubstringOnDottedPaths() {
        dao.save(movie("Inception", "dream within", MovieGenre.ACTION, "Christopher Nolan"));
        dao.save(movie("Dunkirk", "survive", MovieGenre.DRAMA, "Nolan"));
        dao.save(movie("Tenet", "time", MovieGenre.ACTION, "Nolan"));

        PageResult<Movie> action = dao.search(new MovieSearchCriteria(
                List.of(new Filter(MovieFieldPath.GENRE, "ACTION", FilterComparator.EQ)),
                "name:asc", 0, 10));
        assertThat(action.getTotal()).isEqualTo(2);

        PageResult<Movie> nolan = dao.search(new MovieSearchCriteria(
                List.of(new Filter(MovieFieldPath.DIRECTOR_NAME, "Nolan", FilterComparator.SUBSTRING)),
                "name:asc", 0, 10));
        assertThat(nolan.getTotal()).isEqualTo(3);

        Movie heavy = new Movie(0L, "OscarHeavy",
                new Coordinates(1, 1f), LocalDate.of(2020, 1, 1), 5, 100f,
                "x", MovieGenre.FANTASY, new Person("D", null, null, null));
        dao.save(heavy);
        dao.save(movie("OscarLight", "y", MovieGenre.DRAMA, "D"));
        PageResult<Movie> oscarHeavy = dao.search(new MovieSearchCriteria(
                List.of(new Filter(MovieFieldPath.OSCARS_COUNT, "3", FilterComparator.GT)),
                "name:asc", 0, 10));
        assertThat(oscarHeavy.getTotal()).isEqualTo(1);
        assertThat(oscarHeavy.getItems().get(0).getName()).isEqualTo("OscarHeavy");
    }

    @Test
    void substringIsCaseInsensitive() {
        dao.save(movie("Inception", "Dream Within", MovieGenre.ACTION, "Christopher Nolan"));
        dao.save(movie("Arrival", "why", MovieGenre.DRAMA, "Denis Villeneuve"));

        PageResult<Movie> byDirector = dao.search(new MovieSearchCriteria(
                List.of(new Filter(MovieFieldPath.DIRECTOR_NAME, "nOLAN", FilterComparator.SUBSTRING)),
                null, 0, 10));
        assertThat(byDirector.getItems()).extracting(Movie::getName).containsExactly("Inception");

        PageResult<Movie> byTagline = dao.search(new MovieSearchCriteria(
                List.of(new Filter(MovieFieldPath.TAGLINE, "dream w", FilterComparator.SUBSTRING)),
                null, 0, 10));
        assertThat(byTagline.getItems()).extracting(Movie::getName).containsExactly("Inception");
    }

    @Test
    void substringTreatsWildcardsLiterally() {
        dao.save(movie("100% Love", "t", MovieGenre.DRAMA, "D"));
        dao.save(movie("1000 Love", "t", MovieGenre.DRAMA, "D"));
        dao.save(movie("snake_case", "t", MovieGenre.DRAMA, "D"));
        dao.save(movie("snakeXcase", "t", MovieGenre.DRAMA, "D"));

        PageResult<Movie> percent = dao.search(new MovieSearchCriteria(
                List.of(new Filter(MovieFieldPath.NAME, "0%", FilterComparator.SUBSTRING)), null, 0, 10));
        assertThat(percent.getItems()).extracting(Movie::getName).containsExactly("100% Love");

        PageResult<Movie> underscore = dao.search(new MovieSearchCriteria(
                List.of(new Filter(MovieFieldPath.NAME, "e_c", FilterComparator.SUBSTRING)), null, 0, 10));
        assertThat(underscore.getItems()).extracting(Movie::getName).containsExactly("snake_case");
    }

    @Test
    void emptyFiltersReturnsAllAndSortsDesc() {
        dao.save(movie("A", "t", MovieGenre.ACTION, "D"));
        dao.save(movie("B", "t", MovieGenre.DRAMA, "D"));
        dao.save(movie("C", "t", MovieGenre.FANTASY, "D"));

        PageResult<Movie> all = dao.search(new MovieSearchCriteria(
                List.of(), "name:desc", 0, 10));

        assertThat(all.getTotal()).isEqualTo(3);
        assertThat(all.getItems().get(0).getName()).isEqualTo("C");
    }

    @Test
    void findByDirectorGreaterThan() {
        dao.save(movie("A", "t", MovieGenre.ACTION, "Alice"));
        dao.save(movie("B", "t", MovieGenre.ACTION, "Bob"));
        dao.save(movie("C", "t", MovieGenre.ACTION, "Zoe"));

        PageResult<Movie> result = dao.findByDirectorGreaterThan("B", 0, 10);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getItems()).extracting(Movie::getName)
                .containsExactlyInAnyOrder("B", "C");
    }

    @Test
    void findUniqueGenresPaginates() {
        dao.save(movie("A", "t", MovieGenre.ACTION, "D"));
        dao.save(movie("B", "t", MovieGenre.DRAMA, "D"));
        dao.save(movie("C", "t", MovieGenre.FANTASY, "D"));
        dao.save(movie("D", "t", MovieGenre.ACTION, "D"));

        PageResult<MovieGenre> genres = dao.findUniqueGenres(0, 2);

        assertThat(genres.getTotal()).isEqualTo(3);
        assertThat(genres.getItems()).hasSize(2);
    }

    @Test
    void coordinatesAreReusedAndDeletedWhenRefCountReachesZero() {
        long initialCount = coordinatesRepository.count();

        Movie a = dao.save(movie("A", "t", MovieGenre.ACTION, "Shared"));
        Movie b = dao.save(movie("B", "t", MovieGenre.DRAMA, "Shared"));

        assertThat(coordinatesRepository.count()).isEqualTo(initialCount + 1);
        Optional<CoordinatesEntity> coords = coordinatesRepository.findByXAndY(1, 2.5f);
        assertThat(coords).isPresent();
        assertThat(coords.orElseThrow().getRefCount()).isEqualTo(2);

        dao.deleteById(a.getId());
        assertThat(coordinatesRepository.findByXAndY(1, 2.5f))
                .get()
                .extracting(CoordinatesEntity::getRefCount)
                .isEqualTo(1);

        dao.deleteById(b.getId());
        assertThat(coordinatesRepository.findByXAndY(1, 2.5f)).isEmpty();
        assertThat(coordinatesRepository.count()).isEqualTo(initialCount);
    }

    @Test
    void directorIsReusedAcrossMovies() {
        dao.save(movie("A", "t", MovieGenre.ACTION, "Nolan"));
        dao.save(movie("B", "t", MovieGenre.DRAMA, "Nolan"));

        assertThat(personRepository.count()).isEqualTo(1);
        assertThat(personRepository.findByName("Nolan")).isPresent();
    }

    @Test
    void humiliateDirectorsRevokesOscarsSumsPerDirectorAndKeepsRequestOrder() {
        Movie nolanA = dao.save(movie("A", "t", MovieGenre.ACTION, "Nolan"));
        Movie nolanB = dao.save(movie("B", "t", MovieGenre.DRAMA, "Nolan"));
        Movie kubrick = dao.save(movie("C", "t", MovieGenre.ACTION, "Kubrick"));
        dao.save(new Movie(0L, "D", new Coordinates(1, 2.5f), LocalDate.of(2020, 1, 1),
                null, 100f, "t", MovieGenre.FANTASY,
                new Person("Villeneuve", LocalDate.of(1970, 5, 5), EyeColor.BLUE, HairColor.BROWN)));

        List<DirectorsHumiliation> result = dao.humiliateDirectors(
                List.of("Nobody", "Nolan", "Villeneuve"));

        assertThat(result).containsExactly(
                new DirectorsHumiliation("Nobody", 0),
                new DirectorsHumiliation("Nolan", 4),
                new DirectorsHumiliation("Villeneuve", 0));
        // Массовый UPDATE идёт мимо контекста персистентности — сбрасываем кэш первого уровня.
        entityManager.clear();
        assertThat(dao.findById(nolanA.getId()).orElseThrow().getOscarsCount()).isNull();
        assertThat(dao.findById(nolanB.getId()).orElseThrow().getOscarsCount()).isNull();
        assertThat(dao.findById(kubrick.getId()).orElseThrow().getOscarsCount()).isEqualTo(2);
    }

    @Test
    void humiliateDirectorsHandlesDuplicateDirectorNamesInRequest() {
        Movie nolanA = dao.save(movie("A", "t", MovieGenre.ACTION, "Nolan"));
        Movie nolanB = dao.save(movie("B", "t", MovieGenre.DRAMA, "Nolan"));
        dao.save(movie("K", "t", MovieGenre.FANTASY, "Kubrick"));

        List<DirectorsHumiliation> result = dao.humiliateDirectors(List.of("Nolan", "Nolan"));

        // По каждому элементу запроса возвращается запись с полной суммой (2 + 2), без исключений.
        assertThat(result).containsExactly(
                new DirectorsHumiliation("Nolan", 4),
                new DirectorsHumiliation("Nolan", 4));
        entityManager.clear();
        assertThat(dao.findById(nolanA.getId()).orElseThrow().getOscarsCount()).isNull();
        assertThat(dao.findById(nolanB.getId()).orElseThrow().getOscarsCount()).isNull();
    }
}
