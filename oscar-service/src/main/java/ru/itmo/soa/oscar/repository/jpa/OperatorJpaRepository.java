package ru.itmo.soa.oscar.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.soa.oscar.repository.entity.OperatorEntity;

import java.util.Collection;

public interface OperatorJpaRepository extends JpaRepository<OperatorEntity, Long> {

    @Query("""
            select o from OperatorEntity o
            where not exists (
                select f from OperatorFilmEntity f
                where f.operator = o and f.filmId in :filmIds
            )
            """)
    Page<OperatorEntity> findWithoutFilms(@Param("filmIds") Collection<Long> filmIds, Pageable pageable);
}
