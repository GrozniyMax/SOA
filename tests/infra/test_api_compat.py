"""Совместимость lab2 API через gateway: полный CRUD /movie-service/movies.

Контракт — openapi/movie-service.yaml:
  POST /movies -> 201 + Location; GET/PUT/DELETE /movies/{id}; 400/422 на невалидный body.
Обязательные поля MovieRequest: name, coordinates {x, y}, genre, director {name}.
"""

import pytest

pytestmark = pytest.mark.usefixtures("stack_ready")

VALID_MOVIE = {
    "name": "infra-test-movie",
    "coordinates": {"x": 1, "y": 2.0},
    "genre": "DRAMA",
    "director": {"name": "infra-test-director"},
    "oscarsCount": 3,
    "tagline": "from pytest",
}

INVALID_MOVIE = {
    "name": "",  # пустое name -> 400/422
    "coordinates": {"x": 1, "y": 2.0},
    "genre": "DRAMA",
    "director": {"name": "x"},
}


def test_movie_crud_contract(session, gateway_url):
    location = None
    created = None
    try:
        # POST -> 201 + Location
        resp = session.post(f"{gateway_url}/movie-service/movies", json=VALID_MOVIE)
        assert resp.status_code == 201, resp.text
        location = resp.headers.get("Location")
        assert location, "нет заголовка Location в 201-ответе"
        created = resp.json()
        assert created["id"] > 0
        assert created["name"] == VALID_MOVIE["name"]
        assert created["creationDate"]

        # Location возвращается как /movies/{id} (без context-path) —
        # через gateway ходим по /movie-service
        assert location.endswith(f"movies/{created['id']}"), location
        get_url = f"{gateway_url}/movie-service/movies/{created['id']}"
        resp = session.get(get_url)
        assert resp.status_code == 200
        fetched = resp.json()
        assert fetched["id"] == created["id"]

        # PUT -> 200
        updated = dict(VALID_MOVIE, name="infra-test-movie-v2", oscarsCount=5)
        resp = session.put(f"{gateway_url}/movie-service/movies/{created['id']}", json=updated)
        assert resp.status_code == 200, resp.text
        assert resp.json()["name"] == "infra-test-movie-v2"

        # DELETE -> 204
        resp = session.delete(f"{gateway_url}/movie-service/movies/{created['id']}")
        assert resp.status_code == 204
    finally:
        # cleanup: удаляем, если что-то пошло не так в середине сценария
        if created:
            session.delete(f"{gateway_url}/movie-service/movies/{created['id']}")

    # GET после DELETE -> 404
    resp = session.get(f"{gateway_url}/movie-service/movies/{created['id']}")
    assert resp.status_code == 404


def test_movie_invalid_body_rejected(session, gateway_url):
    resp = session.post(f"{gateway_url}/movie-service/movies", json=INVALID_MOVIE)
    assert resp.status_code in (400, 422), f"ожидались 400/422, got {resp.status_code}: {resp.text}"


def test_movie_missing_id_404(session, gateway_url):
    resp = session.get(f"{gateway_url}/movie-service/movies/999999999")
    assert resp.status_code == 404
