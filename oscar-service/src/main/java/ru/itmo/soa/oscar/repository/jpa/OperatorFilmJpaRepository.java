package ru.itmo.soa.oscar.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.soa.oscar.repository.entity.OperatorFilmEntity;

public interface OperatorFilmJpaRepository extends JpaRepository<OperatorFilmEntity, Long> {
}