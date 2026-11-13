"""Config Server выдаёт обоим сервисам их production-конфигурацию.

Формат ответа Spring Cloud Config: {name, profiles, propertySources: [...]},
где каждый PropertySource — содержимое movie-service.yml / oscar-service.yml.
"""

import pytest

pytestmark = pytest.mark.usefixtures("stack_ready")


def _fetch_config(session, config_url, service):
    resp = session.get(f"{config_url}/{service}/default")
    assert resp.status_code == 200, f"{service}: {resp.status_code}"
    body = resp.json()
    assert "propertySources" in body, body.keys()
    return body


def test_movie_service_config(session, config_url):
    body = _fetch_config(session, config_url, "movie-service")
    joined = _sources_text(body)
    # из movie-service.yml: datasource и порт сервера
    assert "spring.datasource.url" in joined
    assert "server.port" in joined
    # контекст-путь lab2-контракта
    assert "server.servlet.context-path" in joined


def test_oscar_service_config(session, config_url):
    body = _fetch_config(session, config_url, "oscar-service")
    joined = _sources_text(body)
    assert "spring.datasource.url" in joined
    assert "server.port" in joined
    # параметры клиента movie-service
    assert "movie.service.url" in joined


def _sources_text(body):
    parts = []
    for source in body["propertySources"]:
        parts.append("\n".join(source.get("source", {}).keys()))
    return "\n".join(parts)
