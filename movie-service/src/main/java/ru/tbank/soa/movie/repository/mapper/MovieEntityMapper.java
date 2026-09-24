package ru.tbank.soa.movie.repository.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.tbank.soa.movie.domain.Movie;
import ru.tbank.soa.movie.repository.entity.MovieEntity;

/**
 * MapStruct-маппер между доменным {@link Movie} и JPA-сущностью {@link MovieEntity}.
 * Вложенные Coordinates/Person и перечисления маппятся автоматически по именам свойств.
 * Поля JPA-сущностей, отсутствующие в домене (id, refCount), игнорируются.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MovieEntityMapper {

    Movie toDomain(MovieEntity entity);

    MovieEntity toEntity(Movie movie);
}