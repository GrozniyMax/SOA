package ru.tbank.soa.oscar.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.soa.oscar.domain.MovieGenre;
import ru.tbank.soa.oscar.repository.entity.AwardEntity;
import ru.tbank.soa.oscar.repository.entity.OperatorEntity;
import ru.tbank.soa.oscar.repository.entity.OperatorFilmEntity;
import ru.tbank.soa.oscar.repository.jpa.AwardJpaRepository;
import ru.tbank.soa.oscar.repository.jpa.OperatorFilmJpaRepository;
import ru.tbank.soa.oscar.repository.jpa.OperatorJpaRepository;

import java.util.List;

/**
 * Сидер начальных данных. Заполняет базу только если она пуста.
 * Данные внутренне согласованы: OperatorFilm.filmId ↔ Award.filmId,
 * а имена режиссёров в Award совпадают с режиссёрами в movie-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final OperatorJpaRepository operatorRepository;
    private final OperatorFilmJpaRepository operatorFilmRepository;
    private final AwardJpaRepository awardRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (operatorRepository.count() > 0) {
            log.info("Data already present, skipping seeding");
            return;
        }

        OperatorEntity op1 = operator("Vilmos Zsigmond");
        OperatorEntity op2 = operator("Roger Deakins");
        OperatorEntity op3 = operator("Emmanuel Lubezki");

        operatorFilm(op1, 1L);
        operatorFilm(op1, 2L);
        operatorFilm(op2, 3L);
        operatorFilm(op3, 4L);

        award(1L, 2, MovieGenre.ACTION, "Christopher Nolan");
        award(2L, 0, MovieGenre.DRAMA, "Christopher Nolan");
        award(3L, 1, MovieGenre.ACTION, "Some Director");
        award(4L, 0, MovieGenre.FANTASY, "Another");

        log.info("Seeded {} operators, {} operator-films, {} awards",
                operatorRepository.count(), operatorFilmRepository.count(), awardRepository.count());
    }

    private OperatorEntity operator(String name) {
        OperatorEntity e = new OperatorEntity();
        e.setName(name);
        return operatorRepository.save(e);
    }

    private OperatorFilmEntity operatorFilm(OperatorEntity operator, long filmId) {
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
}