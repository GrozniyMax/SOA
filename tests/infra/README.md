# Инфраструктурные тесты lab3

pytest-тесты, проверяющие поднятый docker-стек (config/eureka/gateway/haproxy/oscar x2).
Тесты предполагают, что стек **уже запущен** (`make up`).

## Запуск

```bash
make up          # поднять стек и дождаться его готовности
make test-infra  # == python3 -m pytest tests/infra/ -v
```

Зависимости (в venv или `pip install --user`):

```bash
python3 -m pip install -r tests/infra/requirements.txt
```

## Переменные окружения

Базовые URL можно переопределить (значения по умолчанию — из `deploy/.env.example`):

| Переменная     | По умолчанию         | Что проверяет                    |
|----------------|----------------------|----------------------------------|
| `GATEWAY_URL`  | `https://localhost:8443` | Zuul gateway (HTTPS, self-signed) |
| `EUREKA_URL`   | `http://localhost:8761`  | реестр Eureka                     |
| `CONFIG_URL`   | `http://localhost:8888`  | Spring Cloud Config Server        |
| `FRONTEND_URL` | `https://localhost:8444` | nginx-фронтенд                    |

## Что проверяется

- `test_eureka.py` — оба сервиса зарегистрированы и UP; oscar-service — двумя инстансами.
- `test_config_server.py` — `/movie-service/default` и `/oscar-service/default` отдают propertySources с datasource/портами.
- `test_gateway.py` — проксирование movie и oscar через gateway; `GET /oscar/operators/losers` = 200 доказывает цепочку oscar → gateway → movie.
- `test_api_compat.py` — lab2-контракт `/movie-service/movies`: POST 201+Location, GET/PUT/DELETE, 404, 400/422 на невалидном body.
- `test_haproxy.py` — roundrobin между oscar-1 и oscar-2 по заголовку `X-Instance`.

TLS-сертификат gateway самоподписанный — все запросы выполняются с `verify=False`.
Перед первым запросом фикстура `stack_ready` ждёт готовности Eureka и gateway (до ~120s).
