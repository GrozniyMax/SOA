package ru.itmo.soa.oscar.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.soa.oscar.repository.entity.OperatorEntity;
import ru.itmo.soa.oscar.repository.entity.OperatorFilmEntity;
import ru.itmo.soa.oscar.repository.jpa.OperatorFilmJpaRepository;
import ru.itmo.soa.oscar.repository.jpa.OperatorJpaRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final OperatorJpaRepository operatorRepository;
    private final OperatorFilmJpaRepository operatorFilmRepository;

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

        log.info("Seeded {} operators, {} operator-films",
                operatorRepository.count(), operatorFilmRepository.count());
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
}
