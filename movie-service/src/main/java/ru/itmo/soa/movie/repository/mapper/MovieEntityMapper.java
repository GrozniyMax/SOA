package ru.itmo.soa.movie.repository.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.itmo.soa.movie.domain.Movie;
import ru.itmo.soa.movie.repository.entity.MovieEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MovieEntityMapper {

    Movie toDomain(MovieEntity entity);

    MovieEntity toEntity(Movie movie);
}