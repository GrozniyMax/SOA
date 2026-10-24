"""Проксирование через API Gateway (Zuul, HTTPS self-signed).

Маршруты: /movie-service/** -> movie (из Eureka), /oscar/** -> haproxy -> oscar x2.
Запрос GET /oscar/operators/losers внутри oscar-service ходит в movie-service
через тот же gateway — значит 200 доказывает цепочку oscar -> gateway -> movie.
"""

import pytest

pytestmark = pytest.mark.usefixtures("stack_ready")


def test_gateway_movie_genres(session, gateway_url):
    resp = session.get(f"{gateway_url}/movie-service/movies/genres")
    assert resp.status_code == 200
    body = resp.json()
    # GenrePage: {items: [...], total, page, size}
    assert "items" in body and "total" in body
    assert isinstance(body["items"], list)


def test_gateway_oscar_losers(session, gateway_url):
    resp = session.get(f"{gateway_url}/oscar/operators/losers")
    assert resp.status_code == 200
    body = resp.json()
    assert isinstance(body, (list, dict))


def test_gateway_movie_search(session, gateway_url):
    resp = session.post(
        f"{gateway_url}/movie-service/movies/search",
        json={"filters": [], "page": 0, "size": 5},
    )
    assert resp.status_code == 200
    body = resp.json()
    # MoviePage: {items: [...], total, page, size}
    assert "items" in body and "total" in body
    assert isinstance(body["items"], list)
