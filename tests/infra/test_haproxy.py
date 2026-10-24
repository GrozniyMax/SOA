"""Балансировка HAProxy: roundrobin между двумя инстансами oscar-service.

Каждый инстанс добавляет заголовок X-Instance со своим INSTANCE_ID
(oscar-service .../config/InstanceHeaderConfig.java). Если под круглые
запросы увидят только один идентификатор — значит балансировка не работает
(или один из инстансов не отвечает за своим весом).
"""

import os
import subprocess
import time

import pytest

pytestmark = pytest.mark.usefixtures("stack_ready")

COMPOSE = ["docker", "compose", "-f", "deploy/docker-compose.yml", "--env-file", "deploy/.env"]


def test_haproxy_roundrobin_both_instances(session, gateway_url):
    instances = set()
    for _ in range(20):
        resp = session.get(f"{gateway_url}/oscar/operators/losers")
        assert resp.status_code == 200, resp.text
        value = resp.headers.get("X-Instance")
        if value:
            instances.add(value)
    # NOTE: если один инстанс oscar упал/не зарегистрирован, здесь будет
    # только одно значение X-Instance — это тоже падение данного теста.
    assert instances == {"1", "2"}, f"ожидались инстансы 1 и 2, получено: {instances}"


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
        deadline = time.monotonic() + 60
        instances = set()
        while time.monotonic() < deadline and instances != {"1"}:
            resp = session.get(f"{gateway_url}/oscar/operators/losers")
            if resp.status_code == 200 and resp.headers.get("X-Instance"):
                instances.add(resp.headers["X-Instance"])
            time.sleep(2)
        assert instances == {"1"}, (
            f"после stop oscar-2 отвечать должен только инстанс 1, получено: {instances}"
        )
    finally:
        _compose("start", "oscar-2")
    # Инстанс вернулся в ротацию.
    deadline = time.monotonic() + 90
    instances = set()
    while time.monotonic() < deadline and instances != {"1", "2"}:
        resp = session.get(f"{gateway_url}/oscar/operators/losers")
        if resp.status_code == 200 and resp.headers.get("X-Instance"):
            instances.add(resp.headers["X-Instance"])
        time.sleep(3)
    assert instances == {"1", "2"}, f"oscar-2 не вернулся в ротацию: {instances}"
