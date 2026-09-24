package ru.tbank.soa.oscar.repository.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.tbank.soa.oscar.domain.Award;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.repository.entity.AwardEntity;
import ru.tbank.soa.oscar.repository.entity.OperatorEntity;

/**
 * MapStruct-маппер между доменными объектами и JPA-сущностями.
 * OperatorFilm маппится не требуется: он используется только внутри запроса
 * на поиск "неудачников" и при сидировании.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OscarEntityMapper {

    Operator toDomain(OperatorEntity entity);

    OperatorEntity toEntity(Operator operator);

    Award toDomain(AwardEntity entity);

    AwardEntity toEntity(Award award);
}