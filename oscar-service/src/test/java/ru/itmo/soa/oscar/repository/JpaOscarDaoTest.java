package ru.itmo.soa.oscar.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import ru.itmo.soa.oscar.domain.Operator;
import ru.itmo.soa.oscar.domain.PageResult;
import ru.itmo.soa.oscar.repository.dao.JpaOscarDao;
import ru.itmo.soa.oscar.repository.dao.OscarDao;
import ru.itmo.soa.oscar.repository.entity.OperatorEntity;
import ru.itmo.soa.oscar.repository.entity.OperatorFilmEntity;
import ru.itmo.soa.oscar.repository.jpa.OperatorFilmJpaRepository;
import ru.itmo.soa.oscar.repository.jpa.OperatorJpaRepository;
import ru.itmo.soa.oscar.repository.mapper.OscarEntityMapperImpl;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({OscarEntityMapperImpl.class, JpaOscarDao.class})
class JpaOscarDaoTest {

    @Autowired
    private OscarDao dao;

    @Autowired
    private OperatorJpaRepository operatorRepository;

    @Autowired
    private OperatorFilmJpaRepository operatorFilmRepository;

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

    @Test
    void findLosersExcludesOperatorsWithAwardedFilms() {
        operator("NoFilms");
        OperatorEntity noOscars = operator("NoOscars");
        filmOf(noOscars, 100L);
        OperatorEntity missingFilm = operator("MissingFilm");
        filmOf(missingFilm, 999L);
        OperatorEntity winner = operator("Winner");
        filmOf(winner, 100L);
        filmOf(winner, 200L);

        PageResult<Operator> losers = dao.findLosers(Set.of(200L), 0, 10);

        assertThat(losers.total()).isEqualTo(3);
        assertThat(losers.items())
                .extracting(Operator::name)
                .containsExactly("NoFilms", "NoOscars", "MissingFilm");
    }

    @Test
    void findLosersWithoutAwardedFilmsReturnsAllOperators() {
        OperatorEntity a = operator("A");
        filmOf(a, 1L);
        operator("B");

        PageResult<Operator> losers = dao.findLosers(List.of(), 0, 10);

        assertThat(losers.total()).isEqualTo(2);
    }

    @Test
    void findLosersPaginates() {
        for (int i = 0; i < 5; i++) {
            operator("Loser" + i);
        }
        OperatorEntity winner = operator("Winner");
        filmOf(winner, 1L);

        PageResult<Operator> first = dao.findLosers(Set.of(1L), 0, 2);
        assertThat(first.total()).isEqualTo(5);
        assertThat(first.items()).extracting(Operator::name).containsExactly("Loser0", "Loser1");

        PageResult<Operator> last = dao.findLosers(Set.of(1L), 2, 2);
        assertThat(last.items()).extracting(Operator::name).containsExactly("Loser4");
    }
}
