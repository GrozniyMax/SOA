"""Балансировка HAProxy: roundrobin между двумя инстансами oscar-service.

Каждый инстанс добавляет заголовок X-Instance со своим INSTANCE_ID
(oscar-service .../config/InstanceHeaderConfig.java). Если под круглые
запросы увидят только один идентификатор — значит балансировка не работает
(или один из инстансов не отвечает за своим весом).
"""

import pytest

pytestmark = pytest.mark.usefixtures("stack_ready")


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
