"""Балансировка и обнаружение экземпляров Oscar через Eureka."""

import os
import subprocess
import time
from urllib.parse import quote

import pytest
from requests import RequestException

pytestmark = pytest.mark.usefixtures("stack_ready")

COMPOSE = ["docker", "compose", "-f", "deploy/docker-compose.yml", "--env-file", "deploy/.env"]


def test_haproxy_roundrobin_both_instances(session, gateway_url):
    _wait_for_routing(session, gateway_url, {"1", "2"})


def _wait_for_routing(session, gateway_url, expected):
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        try:
            responses = [
                session.get(f"{gateway_url}/oscar/operators/losers", timeout=5)
                for _ in range(6)
            ]
            observed = {(response.status_code, response.headers.get("X-Instance")) for response in responses}
            if observed == {(200, instance) for instance in expected}:
                return
        except RequestException as error:
            observed = str(error)
        time.sleep(2)
    pytest.fail(f"Ожидались инстансы {expected}, последние ответы: {observed}")


def test_haproxy_follows_eureka_status(session, eureka_url, gateway_url):
    """Eureka выводит экземпляр из HAProxy, пока контейнер продолжает работать."""
    _wait_for_routing(session, gateway_url, {"1", "2"})
    instance = quote("oscar-service:2", safe="")
    status_url = f"{eureka_url}/eureka/apps/OSCAR-SERVICE/{instance}/status"
    try:
        response = session.put(status_url, params={"value": "OUT_OF_SERVICE"}, timeout=5)
        response.raise_for_status()
        _wait_for_routing(session, gateway_url, {"1"})
    finally:
        response = session.delete(status_url, params={"value": "UP"}, timeout=5)
        response.raise_for_status()
    _wait_for_routing(session, gateway_url, {"1", "2"})


def _compose(*args):
    subprocess.run(
        COMPOSE + list(args), check=True, capture_output=True, timeout=120,
        cwd=os.path.join(os.path.dirname(__file__), "..", ".."),
    )


def test_haproxy_failover_single_instance(session, gateway_url):
    """Остановка одного инстанса oscar не роняет сервис: HAProxy выводит
    его из ротации по health-check, второй инстанс продолжает отвечать."""
    try:
        _compose("stop", "oscar-2")
        _wait_for_routing(session, gateway_url, {"1"})
    finally:
        _compose("start", "oscar-2")
    _wait_for_routing(session, gateway_url, {"1", "2"})
