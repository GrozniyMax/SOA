# SOA Lab — Movie Collection Service + Oscar Service

Лабораторная работа №3: два веб-сервиса (Spring Boot, DDD) по OpenAPI-спецификациям
на платформе Spring Cloud — Config Server, Eureka, Ribbon, API Gateway (Zuul) —
и два экземпляра oscar-service за HAProxy. Второй сервис управляет «Оскарами»
и вызывает первый через gateway по REST.

## Архитектура

Мультимодульный Gradle-проект (Kotlin DSL). Корневой `build.gradle.kts` задаёт общую
конфигурацию (Boot 3.5 / Java 21); у каждого модуля свой `build.gradle.kts`.
Исключение — `api-gateway`: это «legacy-остров» на Boot 2.3 / Spring Cloud Hoxton / Java 11,
потому что настоящие Zuul Proxy и Ribbon существуют только в линейке Hoxton —
последней, где они ещё не выпилены из Spring Cloud Netflix.

| Модуль | Назначение | Упаковка | Порт | Путь |
|--------|-----------|----------|------|------|
| `movie-service` | CRUD + поиск/сортировка/пагинация коллекции `Movie` | JAR | 8080 | context-path `/movie-service` |
| `oscar-service` (×2) | Операции с «Оскарами»; Feign-вызов movie-service через gateway | JAR | 8081 / 8082 | context-path `/oscar` |
| `config-server` | Spring Cloud Config Server, native-профиль (config-repo/ на FS) | JAR | 8888 | — |
| `eureka-server` | Service Discovery (standalone) | JAR | 8761 | — |
| `api-gateway` | Zuul Proxy + Ribbon + Eureka client; единственная входная точка API (HTTPS) | dist (classpath) | 8443 | — |
| `haproxy` | roundrobin-балансировка oscar-1/oscar-2, stats-страница | docker-образ | 9090 (внутри), stats 8404 | `/oscar` |
| `frontend` | nginx: статика Vite + same-origin проксирование на gateway | docker-образ | 8444 | TLS |

### Топология (docker)

```
Интернет ──► frontend (nginx, HTTPS :8444) ──► api-gateway (Zuul, HTTPS :8443)
                                                  ├─ /movie-service/** ── Ribbon ──► Eureka ──► movie-service :8080
                                                  └─ /oscar/** ──► haproxy (roundrobin :9090) ──► oscar-1 :8081
                                                                                                    └─► oscar-2 :8082
oscar-service ──(Feign, HTTPS)──► gateway :8443/movie-service   # межсервисные вызовы тоже через Zuul
config-server :8888 ◄── все сервисы (spring.config.import)
eureka-server :8761 ◄── все сервисы регистрируются
postgres
```

- Все внешние запросы идут через gateway; напрямую сервисы наружу не публикуются
  (movie/oscar без `ports` в compose).
- Маршрут `/movie-service/**` gateway резолвит через Ribbon+Eureka (по service-id);
  маршрут `/oscar` закреплён на хост `haproxy` — балансировку между инстансами
  oscar делает HAProxy (`deploy/haproxy/haproxy.cfg`, roundrobin + health-check).
- Внутренний трафик между контейнерами — plain HTTP; HTTPS только на gateway и frontend.

### Межсервисные вызовы и truststore

Feign-клиент oscar-service (`MOVIE_SERVICE_URL`) направлен на gateway:
`https://gateway:8443/movie-service` — по заданию **все** вызовы movie-service
проксируются через Zuul. Сертификат gateway самоподписанный, поэтому Feign-клиенту
задаётся truststore: `MOVIE_SERVICE_TRUSTSTORE=/certs/gateway-truststore.p12`
(создаётся `deploy/gen-certs.sh` из сертификата gateway).

### Стек

- Java 21, Spring Boot 3.5, Spring Data JPA, Spring MVC REST;
  **api-gateway** — отдельный остров: Boot 2.3.12 + Spring Cloud Hoxton.SR12 + Java 11
- **OpenAPI codegen** (`org.openapi.generator`): из `openapi/*.yaml` генерируются
  интерфейсы контроллеров и DTO (jakarta, Bean Validation)
- **MapStruct** — маппинг между слоями (DTO↔domain, entity↔domain), **Lombok**
- **Spring Cloud OpenFeign** — вызов movie-service из oscar-service (клиент генерируется из `openapi/movie-service.yaml`)
- **H2** (dev-профиль) / **PostgreSQL** (контейнеры, через Config Server)

## Структура модуля (DDD)

У обоих сервисов единый DDD-слой (пакет `ru.itmo.soa.<service>`):

```
api/          # сгенерированные из OpenAPI интерфейсы (*Api)
dto/          # сгенерированные DTO
controller/   # @RestController implements *Api; mapper/ (DTO↔domain)
domain/       # чистая доменная модель (record/enum), без зависимостей
repository/
  entity/     # JPA-сущности
  jpa/        # JpaRepository + Criteria API / JPQL
  dao/        # DAO-интерфейс (только домен) + реализация
  mapper/     # MapStruct (entity↔domain)
service/      # бизнес-логика; принимает/возвращает домен, работает только через DAO
client/       # (oscar-service) Feign-клиент к movie-service
config/       # @ConfigurationProperties, конфигурация Feign, сидер данных
exception/    # глобальный @RestControllerAdvice → ApiError
```

Принципы:
- Сервис не обращается к JPA-репозиториям напрямую — только через DAO.
- DAO принимает/возвращает доменные объекты и скрывает детали БД.
- Валидация — аннотации Bean Validation в контроллере (`@Valid`/`@Validated`);
  бизнес-слой дополнительно проверяет инварианты (422).
- В `movie-service` координаты и режиссёр — отдельные таблицы, фильм ссылается на них
  через `@ManyToOne`; координаты переиспользуются по значению и ведут счётчик ссылок
  `ref_count` (при 0 — удаляются).

## API и эндпоинты

Спецификации: `openapi/movie-service.yaml`, `openapi/oscar-service.yaml`.
Через gateway к movie-service: `https://localhost:8443/movie-service/...`,
к oscar-service: `https://localhost:8443/oscar/...`.

**movie-service** (context-path `/movie-service`):
- `POST /movies` — создать фильм (201 + Location)
- `POST /movies/search` — выборка: фильтры + сортировка + пагинация в теле
- `GET /movies/{id}` / `PUT /movies/{id}` / `DELETE /movies/{id}`
- `GET /movies/by-max-tagline` — фильм с максимальным tagline
- `GET /movies/by-directors-greater-than?director=...` — фильмы, режиссёр больше заданного
- `POST /movies/directors/humiliate` — батчево отобрать Оскары у фильмов списка режиссёров
- `GET /movies/genres` — уникальные жанры

**oscar-service** (базовый путь `/oscar`):
- `GET /oscar/operators/losers` — операторы, ни один фильм которых не получил Оскара
- `POST /oscar/directors/by-genre/{genre}/humiliate` — отобрать Оскары у фильмов режиссёров, снявших в жанре

Коды ответов: 200/201/204/400/404/405/406/415/422/500, а для oscar-service также
502/503/504 (ошибки вызова movie-service). API лабы №2 сохранено без изменений —
старые клиенты работают через gateway без адаптации.

## Конфигурация (Config Server + env)

Разделение конфигурации:
- **Локальный** `application.yml` сервиса — только `spring.application.name`,
  `spring.config.import: optional:configserver:...` и dev-профиль (H2).
- **Config Server** (native-профиль, `config-repo/`) — всё внешнее: порты,
  context-path, PostgreSQL, параметры Feign, CORS. Приоритет remote PropertySource
  ниже локального, поэтому значения в `config-repo/*.yml` — плейсхолдеры на env-переменные.

Значения задаются в `deploy/.env` (шаблон — `deploy/.env.example`):

| Переменная | Где | По умолчанию |
|-----------|-----|--------------|
| `CONFIG_URL` | все сервисы | `http://localhost:8888` (в compose — `http://config:8888`) |
| `EUREKA_URL` | все сервисы + gateway | `http://localhost:8761/eureka` (в compose — `http://eureka:8761/eureka`) |
| `INSTANCE_ID` | oscar-инстансы | `1` / `2` — различает инстансы в реестре Eureka (`instance-id`) и в заголовке `X-Instance` |
| `MOVIE_PORT` | movie-service (config-repo) | `8080` |
| `OSCAR_PORT` | oscar-инстансы (config-repo) | `8081` / `8082` |
| `MOVIE_DB_URL` / `MOVIE_DB_USERNAME` / `MOVIE_DB_PASSWORD` | movie-service | `jdbc:postgresql://localhost:5432/movie` |
| `OSCAR_DB_URL` / `OSCAR_DB_USERNAME` / `OSCAR_DB_PASSWORD` | oscar-service | `jdbc:postgresql://localhost:5432/oscar` |
| `MOVIE_SERVICE_URL` | oscar-service | `http://localhost:8080` (в compose — `https://gateway:8443/movie-service`) |
| `MOVIE_SERVICE_TRUSTSTORE` / `MOVIE_SERVICE_TRUSTSTORE_PASSWORD` | oscar-service | в compose — `/certs/gateway-truststore.p12` + `KEYSTORE_PASSWORD` |
| `MOVIE_HUMILIATE_BATCH_SIZE` | oscar-service | `10` (размер чанка режиссёров в батчевом humiliate) |
| `CORS_ALLOWED_ORIGINS` | оба сервиса | не задан (CORS выключен) |
| `GATEWAY_PORT` / `FRONTEND_PORT` / `EUREKA_PORT` / `CONFIG_PORT` / `HAPROXY_STATS_PORT` | публикация портов на хост | `8443` / `8444` / `8761` / `8888` / `8404` |
| `KEYSTORE_PASSWORD` | сертификаты | `changeit` |
| `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL | `soa` |
| `*_JAVA_OPTIONS` (CONFIG/EUREKA/MOVIE/OSCAR/GATEWAY) | JVM через `_JAVA_OPTIONS` | см. `.env.example` |
| `JPA_DDL_AUTO` | оба | `update` |

## Сборка и запуск

Всё управляется корневым **Makefile**:

```bash
make build    # bootJar movie/oscar/config/eureka + gatewayDist + сборка фронтенда (Vite)
make certs    # self-signed сертификаты: gateway.p12, gateway-truststore.p12, frontend.crt/.key
make up       # создать .env при отсутствии + поднять docker-стек
make test     # ./gradlew test (юнит-тесты)
make test-infra   # pytest-тесты против поднятого стека (tests/infra/)
make down     # остановить стек
make logs     # docker compose logs -f
make restart  # перезапуск контейнеров
make clean    # gradle clean + down (без удаления volume)
make purge    # ДЕСТРУКТИВНО: down -v (удалить volume БД) + удалить сертификаты
```

`make up` поднимает `deploy/docker-compose.yml`: postgres → config-server → eureka-server →
movie + oscar-1 + oscar-2 → haproxy → gateway → frontend (healthchecks и `depends_on`
задают порядок старта). Конфиги из `config-repo/` монтируются в config-server read-only —
правки конфигурации не требуют пересборки образа.

Точки входа после запуска:

- **frontend**: `https://localhost:8444/` (nginx проксирует `/movie-service/` и `/oscar/`
  на gateway same-origin; фронт собран с `VITE_MOVIE_API=/movie-service`, `VITE_OSCAR_API=/oscar`)
- **gateway (API)**: `https://localhost:8443/movie-service/...`, `https://localhost:8443/oscar/...`
- **Eureka dashboard**: `http://localhost:8761`
- **Config Server**: `http://localhost:8888/<service>/default`
- **HAProxy stats**: `http://localhost:8404/stats`

Сертификаты gateway и frontend самоподписанные: в браузере нужно один раз принять
сертификат `https://localhost:8444/` (клиенты API добавляют gateway в truststore — см. выше).
Для Let's Encrypt: `DOMAIN=... make certs` (см. `deploy/gen-certs.sh`).

## Инфраструктурные тесты (`tests/infra/`)

pytest + requests; проверяют **поднятый** стек (`make up` → `make test-infra`).
Базовые URL переопределяются `GATEWAY_URL`, `EUREKA_URL`, `CONFIG_URL`, `FRONTEND_URL`
(по умолчанию — из `deploy/.env.example`). Покрывают:

- `test_eureka.py` — оба сервиса зарегистрированы и UP; oscar — двумя инстансами
- `test_config_server.py` — `/movie-service/default` и `/oscar-service/default` отдают конфиг
- `test_gateway.py` — проксирование movie и oscar через gateway; `GET /oscar/operators/losers`
  доказывает цепочку oscar → gateway → movie (межсервисный вызов через Zuul)
- `test_api_compat.py` — контракт лабы №2 через gateway: POST 201+Location, GET/PUT/DELETE, 404, 400/422
- `test_haproxy.py` — roundrobin oscar-1/oscar-2 по заголовку `X-Instance`

Зависимости: `python3 -m pip install -r tests/infra/requirements.txt`.
Самоподписанный TLS в тестах принимается (`verify=False`); фикстура `stack_ready`
ждёт готовности Eureka и gateway (до ~120s).

## Соответствие требованиям лабы №3

| Требование | Реализация |
|-----------|------------|
| Config Service | `config-server` (native, `config-repo/`), подключение сервисов через `spring.config.import` |
| Service Discovery (Eureka) | `eureka-server` :8761; регистрация всех сервисов, instance-id с `INSTANCE_ID` |
| Client-side Load Balancer (Ribbon) | Ribbon в api-gateway резолвит `movie-service` через Eureka |
| API Gateway (Zuul) | `api-gateway` :8443 (Boot 2.3/Hoxton — последний стек с Zuul+Ribbon), единая точка входа HTTPS |
| Проксирование всех вызовов через gateway | маршруты Zuul `/movie-service/**` (Ribbon) и `/oscar` (→ haproxy); Feign oscar тоже идёт в gateway |
| Второй экземпляр oscar + балансировка | oscar-1/oscar-2 в compose, HAProxy roundrobin + health-check, stats :8404 |
| Совместимость API лабы №2 | контракт проверяется `tests/infra/test_api_compat.py` через gateway |

## Документация API (Swagger UI)

Интерактивная документация (из спецификаций) — в каталоге `docs/` (Swagger UI с
переключателем между сервисами). Локально:

```bash
python3 -m http.server 8080
# открыть http://localhost:8080/docs/
```

## Архитектура и документация

- [docs/architecture-lab3.md](docs/architecture-lab3.md) — как устроен стек: маршруты запросов,
  Config/Eureka/Feign, legacy-остров api-gateway, развёртывание.
- [docs/architecture-lab3.html](docs/architecture-lab3.html) — интерактивная архитектурная схема
  (открыть в браузере).
- [docs/lab3-variants-comparison.md](docs/lab3-variants-comparison.md) — сравнение двух вариантов
  реализации лабы (этот, план A — «legacy-остров», и полный откат на Hoxton в ветке `lab3-hoxton`).
- [docs/plan-lab3.md](docs/plan-lab3.md) — исходный план реализации (вариант A).
