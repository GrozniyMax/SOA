package ru.itmo.soa.oscar.repository.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.itmo.soa.oscar.domain.Operator;
import ru.itmo.soa.oscar.repository.entity.OperatorEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OscarEntityMapper {

    Operator toDomain(OperatorEntity entity);

    OperatorEntity toEntity(Operator operator);
}
