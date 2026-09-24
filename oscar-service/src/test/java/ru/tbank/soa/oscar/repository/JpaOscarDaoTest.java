package ru.tbank.soa.oscar.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.tbank.soa.oscar.domain.Award;
import ru.tbank.soa.oscar.domain.MovieGenre;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;
import ru.tbank.soa.oscar.repository.dao.JpaOscarDao;
import ru.tbank.soa.oscar.repository.dao.OscarDao;
import ru.tbank.soa.oscar.repository.entity.AwardEntity;
import ru.tbank.soa.oscar.repository.entity.OperatorEntity;
import ru.tbank.soa.oscar.repository.entity.OperatorFilmEntity;
import ru.tbank.soa.oscar.repository.jpa.AwardJpaRepository;
import ru.tbank.soa.oscar.repository.jpa.OperatorFilmJpaRepository;
import ru.tbank.soa.oscar.repository.jpa.OperatorJpaRepository;
import ru.tbank.soa.oscar.repository.mapper.OscarEntityMapperImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Лёгкий интеграционный тест DAO на in-memory H2.
 * Проверяет поиск "неудачников", поиск записей по режиссёрам и обнуление оскаров.
 * Сущности сидятся напрямую через JPA-репозитории (не через DAO).
 */
@DataJpaTest
@Import({OscarEntityMapperImpl.class, JpaOscarDao.class})
class JpaOscarDaoTest {

    @Autowired
    private OscarDao dao;

    @Autowired
    private OperatorJpaRepository operatorRepository;

    @Autowired
    private OperatorFilmJpaRepository operatorFilmRepository;

    @Autowired
    private AwardJpaRepository awardRepository;

    private OperatorEntity operator(String name) {
        OperatorEntity e = new OperatorEntity();
        e.setName(name);
        return operatorRepository.save(e);
    }

    private OperatorFilmEntity filmOf(OperatorEntity operator, long filmId) {
        OperatorFilmEntity e = new OperatorFilmEntity();
        e.setOperator(operator);
        e.setFilmId(filmId);
        return operatorFilmRepository.save(e);
    }

    private AwardEntity award(long filmId, int oscarsCount, MovieGenre genre, String director) {
        AwardEntity e = new AwardEntity();
        e.setFilmId(filmId);
        e.setOscarsCount(oscarsCount);
        e.setGenre(genre);
        e.setDirector(director);
        return awardRepository.save(e);
    }

    @Test
    void findLosersReturnsOperatorsWithoutOscarWinningFilms() {
        // loser1: нет фильмов вовсе — всегда неудачник.
        OperatorEntity loser1 = operator("LoserNoFilms");
        // loser2: фильм есть, но оскаров у него нет.
        OperatorEntity loser2 = operator("LoserZeroOscars");
        filmOf(loser2, 100L);
        award(100L, 0, MovieGenre.DRAMA, "Director A");
        // winner: фильм с oscarsCount > 0 — не неудачник.
        OperatorEntity winner = operator("Winner");
        filmOf(winner, 200L);
        award(200L, 5, MovieGenre.ACTION, "Director B");

        PageResult<Operator> losers = dao.findLosers(0, 10);

        assertThat(losers.total()).isEqualTo(2);
        assertThat(losers.items())
                .extracting(Operator::name)
                .containsExactlyInAnyOrder("LoserNoFilms", "LoserZeroOscars");
    }

    @Test
    void findLosersPaginates() {
        for (int i = 0; i < 5; i++) {
            operator("Loser" + i);
        }
        // Один победитель, который не должен попадать в результаты.
        OperatorEntity winner = operator("Winner");
        filmOf(winner, 1L);
        award(1L, 3, MovieGenre.FANTASY, "Director C");

        PageResult<Operator> first = dao.findLosers(0, 2);
        assertThat(first.total()).isEqualTo(5);
        assertThat(first.items()).hasSize(2);

        PageResult<Operator> last = dao.findLosers(2, 2);
        assertThat(last.items()).hasSize(1);
    }

    @Test
    void findAwardsByDirectorInReturnsOnlyMatchingDirectors() {
        award(1L, 2, MovieGenre.DRAMA, "Nolan");
        award(2L, 4, MovieGenre.ACTION, "Nolan");
        award(3L, 1, MovieGenre.FANTASY, "Villeneuve");
        award(4L, 3, MovieGenre.ADVENTURE, "Spielberg");

        List<Award> awards = dao.findAwardsByDirectorIn(List.of("Nolan", "Spielberg"));

        assertThat(awards).hasSize(3);
        assertThat(awards)
                .extracting(Award::director)
                .containsExactlyInAnyOrder("Nolan", "Nolan", "Spielberg");
    }

    @Test
    void revokeOscarsZeroesOscarsCount() {
        AwardEntity a = award(1L, 5, MovieGenre.DRAMA, "Nolan");
        AwardEntity b = award(2L, 3, MovieGenre.ACTION, "Spielberg");

        dao.revokeOscars(List.of(a.getId(), b.getId()));

        assertThat(awardRepository.findById(a.getId()).orElseThrow().getOscarsCount()).isZero();
        assertThat(awardRepository.findById(b.getId()).orElseThrow().getOscarsCount()).isZero();
    }

    @Test
    void revokeOscarsWithEmptyCollectionIsNoOp() {
        AwardEntity a = award(1L, 5, MovieGenre.DRAMA, "Nolan");

        dao.revokeOscars(List.of());

        assertThat(awardRepository.findById(a.getId()).orElseThrow().getOscarsCount()).isEqualTo(5);
    }
}