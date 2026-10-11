package ru.itmo.soa.oscar.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.itmo.soa.oscar.domain.Movie;
import ru.itmo.soa.oscar.domain.MovieGenre;
import ru.itmo.soa.oscar.service.exception.MovieServiceBadGatewayException;
import ru.itmo.soa.oscar.service.exception.MovieServiceUnavailableException;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebMovieServiceClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Recorded> requests = new ArrayList<>();

    private WebMovieServiceClient client(Function<Recorded, ClientResponse> responder) {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://movie")
                .exchangeFunction(request -> {
                    Recorded recorded = new Recorded(request.method(), request.url().getPath(), body(request));
                    requests.add(recorded);
                    return Mono.just(responder.apply(recorded));
                })
                .build();
        return new WebMovieServiceClient(webClient);
    }

    private JsonNode body(ClientRequest request) {
        MockClientHttpRequest httpRequest = new MockClientHttpRequest(request.method(), request.url());
        @SuppressWarnings("unchecked")
        BodyInserter<?, ClientHttpRequest> inserter = (BodyInserter<?, ClientHttpRequest>) request.body();
        inserter.insert(httpRequest, new BodyInserter.Context() {
            @Override
            public List<HttpMessageWriter<?>> messageWriters() {
                return ExchangeStrategies.withDefaults().messageWriters();
            }

            @Override
            public Optional<ServerHttpRequest> serverRequest() {
                return Optional.empty();
            }

            @Override
            public Map<String, Object> hints() {
                return Map.of();
            }
        }).block();
        String text = httpRequest.getBodyAsString().block();
        try {
            return text == null || text.isEmpty() ? null : objectMapper.readTree(text);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private String page(long total, String... items) {
        return "{\"items\":[" + String.join(",", items) + "],\"total\":" + total + "}";
    }

    private String movie(long id, Integer oscars, String director) {
        return "{\"id\":" + id + ",\"oscarsCount\":" + oscars + ",\"director\":{\"name\":\"" + director + "\"}}";
    }

    @Test
    void findDirectorsByGenreWalksAllPages() {
        WebMovieServiceClient client = client(r -> {
            int page = r.body().get("page").asInt();
            List<String> items = new ArrayList<>();
            int count = page == 0 ? 100 : 5;
            for (int i = 0; i < count; i++) {
                items.add(movie(page * 100L + i + 1, null, page == 0 ? "Nolan" : "Villeneuve"));
            }
            return json(HttpStatus.OK, page(105, items.toArray(String[]::new)));
        });

        assertThat(client.findDirectorsByGenre(MovieGenre.ACTION)).containsExactlyInAnyOrder("Nolan", "Villeneuve");
        assertThat(requests).hasSize(2);
        JsonNode first = requests.get(0).body();
        assertThat(first.get("filters").get(0).get("path").asText()).isEqualTo("genre");
        assertThat(first.get("filters").get(0).get("value").asText()).isEqualTo("ACTION");
        assertThat(first.get("size").asInt()).isEqualTo(100);
        assertThat(requests.get(1).body().get("page").asInt()).isEqualTo(1);
    }

    @Test
    void findAwardedMoviesByDirectorFiltersByDirectorAndOscars() {
        WebMovieServiceClient client = client(r -> json(HttpStatus.OK, page(2, movie(1, 2, "Nolan"), movie(7, 4, "Nolan"))));

        List<Movie> movies = client.findAwardedMoviesByDirector("Nolan");

        assertThat(movies).containsExactly(new Movie(1L, 2), new Movie(7L, 4));
        JsonNode filters = requests.get(0).body().get("filters");
        assertThat(filters.get(0).get("path").asText()).isEqualTo("director.name");
        assertThat(filters.get(0).get("value").asText()).isEqualTo("Nolan");
        assertThat(filters.get(1).get("path").asText()).isEqualTo("oscarsCount");
        assertThat(filters.get(1).get("comparator").asText()).isEqualTo("gt");
    }

    @Test
    void findAwardedMovieIdsReturnsIds() {
        WebMovieServiceClient client = client(r -> json(HttpStatus.OK, page(2, movie(3, 1, "A"), movie(9, 5, "B"))));

        assertThat(client.findAwardedMovieIds()).containsExactlyInAnyOrder(3L, 9L);
    }

    @Test
    void revokeOscarsPutsFullMovieWithNullOscars() {
        String stored = """
                {"id":5,"name":"Dune","coordinates":{"x":1,"y":2.5},"creationDate":"2026-01-01",
                 "oscarsCount":6,"totalBoxOffice":100.0,"tagline":"Fear","genre":"FANTASY",
                 "director":{"name":"Villeneuve","birthday":"1967-10-03","eyeColor":"BLUE","hairColor":"BROWN"}}
                """;
        WebMovieServiceClient client = client(r -> json(HttpStatus.OK, stored));

        client.revokeOscars(5L);

        assertThat(requests).hasSize(2);
        Recorded put = requests.get(1);
        assertThat(put.method()).isEqualTo(HttpMethod.PUT);
        assertThat(put.path()).isEqualTo("/movies/5");
        JsonNode body = put.body();
        assertThat(body.has("id")).isFalse();
        assertThat(body.has("creationDate")).isFalse();
        assertThat(body.get("oscarsCount").isNull()).isTrue();
        assertThat(body.get("name").asText()).isEqualTo("Dune");
        assertThat(body.get("tagline").asText()).isEqualTo("Fear");
        assertThat(body.get("director").get("birthday").asText()).isEqualTo("1967-10-03");
    }

    @Test
    void errorStatusMapsToBadGateway() {
        WebMovieServiceClient client = client(r -> json(HttpStatus.UNPROCESSABLE_ENTITY, "{\"status\":422,\"message\":\"bad\"}"));

        assertThatThrownBy(() -> client.revokeOscars(1L))
                .isInstanceOf(MovieServiceBadGatewayException.class)
                .hasMessageContaining("422");
    }

    @Test
    void connectionRefusedMapsToUnavailable() {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://movie")
                .exchangeFunction(request -> Mono.error(new ConnectException("refused")))
                .build();
        WebMovieServiceClient client = new WebMovieServiceClient(webClient);

        assertThatThrownBy(client::findAwardedMovieIds).isInstanceOf(MovieServiceUnavailableException.class);
    }

    private record Recorded(HttpMethod method, String path, JsonNode body) {
    }
}
