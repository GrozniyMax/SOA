package ru.tbank.soa.oscar.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.tbank.soa.oscar.repository.entity.OperatorEntity;

/**
 * Spring Data JPA-репозиторий операторов.
 */
public interface OperatorJpaRepository extends JpaRepository<OperatorEntity, Long> {

    /**
     * Возвращает "неудачников" — операторов, у которых нет ни одного фильма с oscarsCount > 0
     * (включая операторов без фильмов вовсе).
     */
    @Query("""
            select distinct o from OperatorEntity o
            where not exists (
                select of from OperatorFilmEntity of
                where of.operator = o
                  and exists (select a from AwardEntity a where a.filmId = of.filmId and a.oscarsCount > 0)
            )
            """)
    Page<OperatorEntity> findLosers(Pageable pageable);
}