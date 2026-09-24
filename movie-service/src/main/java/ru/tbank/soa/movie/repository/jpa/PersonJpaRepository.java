package ru.tbank.soa.movie.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tbank.soa.movie.repository.entity.PersonEntity;

import java.util.Optional;

/**
 * Spring Data JPA-репозиторий сущности {@link PersonEntity} (режиссёр).
 * Один режиссёр переиспользуется несколькими фильмами — поиск по имени (natural key).
 */
public interface PersonJpaRepository extends JpaRepository<PersonEntity, Long> {

    Optional<PersonEntity> findByName(String name);
}