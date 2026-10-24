package ru.itmo.soa.movie.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.soa.movie.domain.MovieGenre;
import ru.itmo.soa.movie.repository.entity.MovieEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA-репозиторий сущности {@link MovieEntity}.
 * Работает только с JPA-сущностями; доменные объекты наружу не выдаёт.
 */
public interface MovieJpaRepository
        extends JpaRepository<MovieEntity, Long>,
        JpaSpecificationExecutor<MovieEntity> {

    @Override
    @EntityGraph(attributePaths = {"coordinates", "director"})
    Optional<MovieEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"coordinates", "director"})
    Page<MovieEntity> findAll(
            Specification<MovieEntity> specification,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"coordinates", "director"})
    Optional<MovieEntity> findFirstByTaglineIsNotNullOrderByTaglineDesc();

    @EntityGraph(attributePaths = {"coordinates", "director"})
    Page<MovieEntity> findByDirector_NameGreaterThan(
            String director,
            Pageable pageable
    );

    @Query("select distinct m.genre from MovieEntity m")
    List<MovieGenre> findDistinctGenres();

    @Query("SELECT m.director.name, SUM(m.oscarsCount) FROM MovieEntity m "
            + "WHERE m.director.name IN :directors AND m.oscarsCount > 0 GROUP BY m.director.name")
    List<Object[]> sumOscarsByDirectors(@Param("directors") Collection<String> directors);

    // Hibernate 5 не поддерживает неявные join'ы в bulk-update HQL
    // (m.director.name генерирует невалидный «cross join»), поэтому режиссёры
    // выбираются подзапросом; семантика та же, что в Hibernate 6.
    @Modifying
    @Query("UPDATE MovieEntity m SET m.oscarsCount = null "
            + "WHERE m.oscarsCount > 0 AND m.director.id IN ("
            + "    SELECT p.id FROM PersonEntity p WHERE p.name IN :directors)")
    int nullOscarsByDirectors(@Param("directors") Collection<String> directors);
}
