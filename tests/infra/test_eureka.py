"""Проверка реестра Eureka: оба сервиса зарегистрированы и UP.

oscar-service должен быть представлен ДВУМЯ инстансами (oscar-1, oscar-2) —
иначе HAProxy балансирует в один инстанс (см. test_haproxy.py).
"""

import pytest

pytestmark = pytest.mark.usefixtures("stack_ready")


def _apps_by_name(session, eureka_url):
    resp = session.get(f"{eureka_url}/eureka/apps", headers={"Accept": "application/json"})
    assert resp.status_code == 200
    return {app["name"]: app for app in resp.json()["applications"]["application"]}


def test_eureka_apps_available(session, eureka_url):
    resp = session.get(f"{eureka_url}/eureka/apps", headers={"Accept": "application/json"})
    assert resp.status_code == 200
    body = resp.json()
    assert "applications" in body


def test_movie_service_registered_up(session, eureka_url):
    apps = _apps_by_name(session, eureka_url)
    assert "MOVIE-SERVICE" in apps
    instances = apps["MOVIE-SERVICE"]["instance"]
    up = [i for i in instances if i["status"] == "UP"]
    assert len(up) >= 1, f"movie-service должен быть UP, got {[i['status'] for i in instances]}"


def test_oscar_service_two_up_instances(session, eureka_url):
    apps = _apps_by_name(session, eureka_url)
    assert "OSCAR-SERVICE" in apps
    instances = apps["OSCAR-SERVICE"]["instance"]
    up = [i for i in instances if i["status"] == "UP"]
    statuses = [i["status"] for i in instances]
    assert len(up) == 2, f"ожидались 2 UP-инстанса oscar-service, got {statuses}"
    ids = sorted(i["instanceId"] for i in up)
    assert any(i.endswith(":1") for i in ids), f"нет инстанса с INSTANCE_ID=1: {ids}"
    assert any(i.endswith(":2") for i in ids), f"нет инстанса с INSTANCE_ID=2: {ids}"
