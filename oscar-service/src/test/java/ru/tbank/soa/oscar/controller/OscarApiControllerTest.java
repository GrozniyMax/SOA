package ru.tbank.soa.oscar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.soa.oscar.controller.mapper.OscarApiMapperImpl;
import ru.tbank.soa.oscar.domain.HumiliationResult;
import ru.tbank.soa.oscar.domain.MovieGenre;
import ru.tbank.soa.oscar.domain.Operator;
import ru.tbank.soa.oscar.domain.PageResult;
import ru.tbank.soa.oscar.service.OscarService;
import ru.tbank.soa.oscar.service.exception.MovieServiceBadGatewayException;
import ru.tbank.soa.oscar.service.exception.MovieServiceGatewayTimeoutException;
import ru.tbank.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-слой тест контроллера: проверяет маппинг эндпоинтов, отображение домена в DTO
 * через реальный MapStruct-маппер, а также глобальный обработчик исключений, включая
 * 502/503/504 при сбоях movie-service. Сервис замокан, маппер — реальный.
 */
@WebMvcTest(OscarApiController.class)
@Import(OscarApiMapperImpl.class)
class OscarApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OscarService service;

    @Test
    void getLosersReturnsMappedPage() throws Exception {
        when(service.getLosers(0, 5))
                .thenReturn(new PageResult<>(List.of(new Operator(1L, "Deakins")), 42, 0, 5));

        mockMvc.perform(get("/operators/losers").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(42))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Deakins"));
    }

    @Test
    void humiliateDirectorsByGenreReturnsMappedResult() throws Exception {
        when(service.humiliateDirectorsByGenre(MovieGenre.ACTION))
                .thenReturn(new HumiliationResult(MovieGenre.ACTION, 2, 3));

        mockMvc.perform(post("/directors/by-genre/ACTION/humiliate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genre").value("ACTION"))
                .andExpect(jsonPath("$.affectedDirectors").value(2))
                .andExpect(jsonPath("$.revokedOscars").value(3));
    }

    @Test
    void movieServiceUnavailableMapsTo503() throws Exception {
        when(service.humiliateDirectorsByGenre(any()))
                .thenThrow(new MovieServiceUnavailableException("movie-service down"));

        mockMvc.perform(post("/directors/by-genre/ACTION/humiliate"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void movieServiceGatewayTimeoutMapsTo504() throws Exception {
        when(service.humiliateDirectorsByGenre(any()))
                .thenThrow(new MovieServiceGatewayTimeoutException("movie-service timeout"));

        mockMvc.perform(post("/directors/by-genre/ACTION/humiliate"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value(504));
    }

    @Test
    void movieServiceBadGatewayMapsTo502() throws Exception {
        when(service.humiliateDirectorsByGenre(any()))
                .thenThrow(new MovieServiceBadGatewayException("movie-service bad response"));

        mockMvc.perform(post("/directors/by-genre/ACTION/humiliate"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void invalidGenrePathValueReturns400() throws Exception {
        mockMvc.perform(post("/directors/by-genre/XYZ/humiliate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}