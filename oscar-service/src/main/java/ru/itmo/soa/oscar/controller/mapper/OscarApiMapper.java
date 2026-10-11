package ru.itmo.soa.oscar.controller.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.itmo.soa.oscar.domain.HumiliationResult;
import ru.itmo.soa.oscar.domain.MovieGenre;
import ru.itmo.soa.oscar.domain.Operator;
import ru.itmo.soa.oscar.domain.PageResult;
import ru.itmo.soa.oscar.dto.Page;

/**
 * MapStruct-маппер между DTO из OpenAPI-спецификации и доменной моделью Oscar Service.
 * DTO-типы указываются полными именами, чтобы избежать неоднозначности с доменными
 * типами, носящими те же простые имена (Operator, HumiliationResult, MovieGenre, PageResult).
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OscarApiMapper {

    ru.itmo.soa.oscar.dto.Operator toDto(Operator operator);

    Page toPage(PageResult<Operator> page);

    ru.itmo.soa.oscar.dto.HumiliationResult toDto(HumiliationResult result);

    MovieGenre toDomain(ru.itmo.soa.oscar.dto.MovieGenre genre);
}