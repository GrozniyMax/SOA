package ru.itmo.soa.oscar.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.soa.oscar.repository.entity.OperatorFilmEntity;

/**
 * Spring Data JPA-репозиторий связей оператор-фильм.
 * Используется, в частности, сидером данных; дополнительных методов пока не требуется.
 */
public interface OperatorFilmJpaRepository extends JpaRepository<OperatorFilmEntity, Long> {
}