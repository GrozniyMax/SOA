package ru.tbank.soa.oscar.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.tbank.soa.oscar.repository.entity.AwardEntity;

import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA-репозиторий оскарных записей.
 */
public interface AwardJpaRepository extends JpaRepository<AwardEntity, Long> {

    List<AwardEntity> findByDirectorIn(Collection<String> directors);

    /**
     * Обнуляет количество оскаров у указанных записей (используется при "унижении").
     * Бulk-запрос обходит first-level cache, поэтому контекст персистентности очищается,
     * чтобы последующие чтения видели обновлённые значения.
     */
    @Modifying(clearAutomatically = true)
    @Query("update AwardEntity a set a.oscarsCount = 0 where a.id in :ids")
    int revokeOscars(@Param("ids") Collection<Long> ids);
}