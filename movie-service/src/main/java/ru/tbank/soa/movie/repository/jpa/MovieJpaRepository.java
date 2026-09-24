package ru.tbank.soa.movie.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.tbank.soa.movie.domain.MovieGenre;
import ru.tbank.soa.movie.repository.entity.MovieEntity;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA-репозиторий сущности {@link MovieEntity}.
 * Работает только с JPA-сущностями; доменные объекты наружу не выдаёт.
 */
public interface MovieJpaRepository
        extends JpaRepository<MovieEntity, Long>, JpaSpecificationExecutor<MovieEntity> {

    Optional<MovieEntity> findFirstByTaglineIsNotNullOrderByTaglineDesc();

    Page<MovieEntity> findByDirector_NameGreaterThan(String director, Pageable pageable);

    @Query("select distinct m.genre from MovieEntity m")
    List<MovieGenre> findDistinctGenres();
}