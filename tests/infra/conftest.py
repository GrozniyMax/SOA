"""Общие фикстуры инфраструктурных тестов lab3.

Базовые URL берутся из переменных окружения (значения по умолчанию
соответствуют deploy/.env.example). TLS у gateway/frontend — self-signed,
поэтому verify=False во всех запросах.
"""

import os
import time

import pytest
import requests
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

GATEWAY_URL = os.environ.get("GATEWAY_URL", "https://localhost:8443")
EUREKA_URL = os.environ.get("EUREKA_URL", "http://localhost:8761")
CONFIG_URL = os.environ.get("CONFIG_URL", "http://localhost:8888")
FRONTEND_URL = os.environ.get("FRONTEND_URL", "https://localhost:8444")


@pytest.fixture(scope="session")
def gateway_url():
    return GATEWAY_URL.rstrip("/")


@pytest.fixture(scope="session")
def eureka_url():
    return EUREKA_URL.rstrip("/")


@pytest.fixture(scope="session")
def config_url():
    return CONFIG_URL.rstrip("/")


@pytest.fixture(scope="session")
def frontend_url():
    return FRONTEND_URL.rstrip("/")


@pytest.fixture(scope="session")
def session():
    """HTTP-сессия без верификации TLS (self-signed сертификат gateway)."""
    s = requests.Session()
    s.verify = False
    yield s
    s.close()


def wait_for(url, timeout=120, poll_interval=3, session=None):
    """Ждём, пока URL начнёт отвечать (любым кодом < 500)."""
    s = session or requests
    deadline = time.monotonic() + timeout
    last_error = None
    while time.monotonic() < deadline:
        try:
            resp = s.get(url, verify=False, timeout=5)
            if resp.status_code < 500:
                return resp
            last_error = f"status {resp.status_code}"
        except requests.RequestException as exc:
            last_error = str(exc)
        time.sleep(poll_interval)
    raise TimeoutError(f"{url} не отвечает за {timeout}s: {last_error}")


@pytest.fixture(scope="session")
def stack_ready(session, eureka_url, gateway_url):
    """Стек должен быть поднят (make up): ждём Eureka и gateway HTTPS.

    Сервисы регистрируются в Eureka не мгновенно, поэтому общий таймаут ~120s.
    """
    wait_for(f"{eureka_url}/eureka/apps", timeout=120, session=session)
    wait_for(gateway_url, timeout=120, session=session)
