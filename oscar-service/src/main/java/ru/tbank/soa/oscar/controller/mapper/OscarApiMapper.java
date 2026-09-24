package ru.tbank.soa.oscar.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.tbank.soa.oscar.domain.HumiliationResult;
import ru.tbank.soa.oscar.domain.MovieGenre;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;

/**
 * MapStruct-маппер между DTO из OpenAPI-спецификации и доменной моделью Oscar Service.
 * DTO-типы указываются полными именами, чтобы избежать неоднозначности с доменными
 * типами, носящими те же простые имена (Operator, HumiliationResult, MovieGenre, PageResult).
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OscarApiMapper {

    ru.tbank.soa.oscar.dto.Operator toDto(Operator operator);

    ru.tbank.soa.oscar.dto.Page toPage(PageResult<Operator> page);

    ru.tbank.soa.oscar.dto.HumiliationResult toDto(HumiliationResult result);

    MovieGenre toDomain(ru.tbank.soa.oscar.dto.MovieGenre genre);
}