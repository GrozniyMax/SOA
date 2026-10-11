package ru.itmo.soa.movie.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.soa.movie.repository.entity.CoordinatesEntity;

import java.util.Optional;

/**
 * Spring Data JPA-репозиторий сущности {@link CoordinatesEntity}.
 * Нужен для переиспользования координат по значению (x, y) и управления счётчиком ссылок.
 */
public interface CoordinatesJpaRepository extends JpaRepository<CoordinatesEntity, Long> {

    Optional<CoordinatesEntity> findByXAndY(int x, float y);
}