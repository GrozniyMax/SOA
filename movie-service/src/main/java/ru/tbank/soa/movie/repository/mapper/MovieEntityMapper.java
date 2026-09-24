package ru.tbank.soa.movie.repository.mapper;

import org.mapstruct.Mapper;
import ru.tbank.soa.movie.domain.Movie;
import ru.tbank.soa.movie.repository.entity.MovieEntity;

/**
 * MapStruct-маппер между доменным {@link Movie} и JPA-сущностью {@link MovieEntity}.
 * Вложенные Coordinates/Person и перечисления маппятся автоматически по именам свойств.
 */
@Mapper(componentModel = "spring")
public interface MovieEntityMapper {

    Movie toDomain(MovieEntity entity);

    MovieEntity toEntity(Movie movie);
}