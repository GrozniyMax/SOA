package ru.itmo.soa.movie.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.soa.movie.repository.entity.CoordinatesEntity;

import java.util.Optional;

public interface CoordinatesJpaRepository extends JpaRepository<CoordinatesEntity, Long> {

    Optional<CoordinatesEntity> findByXAndY(int x, float y);
}