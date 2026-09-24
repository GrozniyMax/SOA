package ru.tbank.soa.movie.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.tbank.soa.movie.controller.mapper.MovieApiMapperImpl;
import ru.tbank.soa.movie.domain.Coordinates;
import ru.tbank.soa.movie.domain.EyeColor;
import ru.tbank.soa.movie.domain.Movie;
import ru.tbank.soa.movie.domain.MovieGenre;
import ru.tbank.soa.movie.domain.Person;
import ru.tbank.soa.movie.service.MovieService;
import ru.tbank.soa.movie.service.exception.MovieNotFoundException;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-слой тест контроллера: проверяет маппинг эндпоинтов, валидацию (@Valid → 422)
 * и глобальный обработчик исключений. Сервис замокан, маппер — реальный.
 */
@WebMvcTest(MoviesApiController.class)
@Import(MovieApiMapperImpl.class)
class MoviesApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService service;

    @Test
    void addMovieReturns201WithLocationAndBody() throws Exception {
        Movie created = new Movie(5L, "Inception", new Coordinates(1, 2.5f), LocalDate.of(2026, 9, 24),
                2, null, "dream", MovieGenre.ACTION, new Person("Nolan", null, EyeColor.BLUE, null));
        when(service.addMovie(any())).thenReturn(created);

        mockMvc.perform(post("/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Inception","coordinates":{"x":1,"y":2.5},"genre":"ACTION",
                                 "oscarsCount":2,"tagline":"dream","director":{"name":"Nolan","eyeColor":"BLUE"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/movies/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Inception"));
    }

    @Test
    void addMovieReturns422ForBlankName() throws Exception {
        mockMvc.perform(post("/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","coordinates":{"x":1,"y":2.5},"genre":"ACTION",
                                 "director":{"name":"Nolan"}}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void getMovieByIdReturns404WhenNotFound() throws Exception {
        when(service.getMovieById(99L)).thenThrow(new MovieNotFoundException(99L));

        mockMvc.perform(get("/movies/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}