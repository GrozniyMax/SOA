package ru.itmo.soa.oscar.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import ru.itmo.soa.oscar.api.DirectorsApi;
import ru.itmo.soa.oscar.api.OperatorsApi;
import ru.itmo.soa.oscar.controller.mapper.OscarApiMapper;
import ru.itmo.soa.oscar.dto.HumiliationResult;
import ru.itmo.soa.oscar.dto.MovieGenre;
import ru.itmo.soa.oscar.dto.Page;
import ru.itmo.soa.oscar.service.OscarService;

import java.util.Optional;

/**
 * Web-контроллер Oscar Service: реализует оба сгенерированных OpenAPI-интерфейса.
 * Принимает/возвращает сгенерированные DTO, бизнес-логика выполняется только через
 * {@link OscarService}, маппинг DTO &lt;-&gt; домен выполняется {@link OscarApiMapper}.
 */
@Validated
@RestController
@RequiredArgsConstructor
public class OscarApiController implements OperatorsApi, DirectorsApi {

    private final OscarService service;
    private final OscarApiMapper mapper;

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    @Override
    public ResponseEntity<Page> getLosers(Integer page, Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return ResponseEntity.ok(mapper.toPage(service.getLosers(p, s)));
    }

    @Override
    public ResponseEntity<HumiliationResult> humiliateDirectorsByGenre(MovieGenre genre) {
        return ResponseEntity.ok(mapper.toDto(service.humiliateDirectorsByGenre(mapper.toDomain(genre))));
    }
}