package ru.itmo.soa.movie.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.soa.movie.repository.entity.PersonEntity;

import java.util.Optional;

public interface PersonJpaRepository extends JpaRepository<PersonEntity, Long> {

    Optional<PersonEntity> findByName(String name);
}