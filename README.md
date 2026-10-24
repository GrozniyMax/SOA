# SOA Lab — Movie Collection Service + Oscar Service

Лабораторная работа №3: два веб-сервиса (Spring Boot, DDD) по OpenAPI-спецификациям
на платформе Spring Cloud — Config Server, Eureka, Ribbon, API Gateway (Zuul) —
и два экземпляра oscar-service за HAProxy. Второй сервис управляет «Оскарами»
и вызывает первый через gateway по REST.

**Вся платформа — единый стек: Spring Boot 2.3.12 + Spring Cloud Hoxton.SR12 + Java 11**
(эпоха `javax`, Hibernate 5). Настоящие Zuul Proxy и Ribbon существуют
только в линейке Hoxton — последней, где они ещё не выпилены из Spring Cloud
Netflix, — поэтому на Hoxton переведён не только gateway, но и оба сервиса
(см. [docs/architecture-hoxton.md](docs/architecture-hoxton.md)).

## Архитектура

Мультимодульный Gradle-проект (Kotlin DSL). Корневой `build.gradle.kts` задаёт
общую конфигурацию: Java 11, BOM-ы Boot 2.3.12 / Hoxton.SR12 / feign 10.12,
classpath-упаковку `appDist`; у каждого модуля свой `build.gradle.kts`.

| Модуль | Назначение | Упаковка | Порт | Путь |
|--------|-----------|----------|------|------|
| `movie-service` | CRUD + поиск/сортировка/пагинация коллекции `Movie` | dist (classpath) | 8080 | context-path `/movie-service` |
| `oscar-service` (×2) | Операции с «Оскарами»; Feign-вызов movie-service через gateway | dist (classpath) | 8081 / 8082 | context-path `/oscar` |
| `config-server` | Spring Cloud Config Server, native-профиль (config-repo/ на FS) | dist (classpath) | 8888 | — |
| `eureka-server` | Service Discovery (standalone) | dist (classpath) | 8761 | — |
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
config-server :8888 ◄── все сервисы (bootstrap.yml, Hoxton)
eureka-server :8761 ◄── все сервисы регистрируются
postgres
```

- Все внешние запросы идут через gateway; напрямую сервисы наружу не публикуются
  (movie/oscar без `ports` в compose).
- Маршрут `/movie-service/**` gateway резолвит через Ribbon+Eureka (по service-id,
  `strip-prefix: false` — путь уходит целиком, т.к. movie слушает с context-path);
  маршрут `/oscar` закреплён на хост `haproxy` — балансировку между инстансами
  oscar делает HAProxy (`deploy/haproxy/haproxy.cfg`, roundrobin + health-check).
- Внутренний трафик между контейнерами — plain HTTP; HTTPS только на gateway и frontend.

### Межсервисные вызовы и truststore

Feign-клиент oscar-service (`MOVIE_SERVICE_URL`) направлен на gateway:
`https://gateway:8443/movie-service` — по заданию **все** вызовы movie-service
проксируются через Zuul. Сертификат gateway самоподписанный, поэтому Feign-клиенту
задаётся truststore: `MOVIE_SERVICE_TRUSTSTORE=/certs/gateway-truststore.p12`
(создаётся `deploy/gen-certs.sh` из сертификата gateway).

## Единый стек: Boot 2.3 / Hoxton / Java 11

Проект собран целиком на последнем стеке, где ещё есть Zuul и Ribbon, а не на
смеси «Boot 3 + legacy-остров»: одна платформа, один набор BOM-ов в корневом
билде. Что из этого следует:

- **javax вместо jakarta** — сгенерированные из OpenAPI интерфейсы и DTO используют
  `javax.validation`; Spring MVC / Hibernate 5 из Boot 2.3 им соответствуют.
- **Hibernate 5** — в bulk-update HQL нет неявных join'ов (`m.director.name` в
  `UPDATE` дал бы невалидный cross join), поэтому батчевый отзыв Оскаров
  (`nullOscarsByDirectors` в `MovieJpaRepository`) выбирает режиссёров
  **подзапросом** `m.director.id IN (SELECT p.id FROM PersonEntity p ...)`.
- **OpenFeign 10.12** (пинится Hoxton через вложенный feign-bom): поддерживает
  только одноуровневое наследование — `@FeignClient`-интерфейс не может
  наследовать методы у родительского интерфейса, а сгенерированный `MoviesApi`
  сам наследует `ApiClient.Api`. Поэтому клиент `MovieServiceFeignClient`
  написан вручную: нужные методы переобъявлены с нативными feign-аннотациями
  (`@RequestLine`, `@Headers`) поверх тех же сгенерированных DTO; контракт —
  `feign.Contract.Default`.
- **@MockBean вместо @MockitoBean** — в тестах контроллеров (Spring Test из
  Boot 2.3 не знает `@MockitoBean` из Boot 3.4+).

## Сборка и упаковка (classpath-dist, без fat-jar)

Градle-плагин Spring Boot 2.x несовместим с Gradle 8.14
(`LazyPublishArtifact NoSuchMethodError`), поэтому **плагин Boot не применяется
ни в одном модуле**. Версии пинятся BOM-ами в корневом `build.gradle.kts`:

- `spring-boot-dependencies:2.3.12.RELEASE` и `spring-cloud-dependencies:Hoxton.SR12`
  импортируются **явно** — плагин dependency-management не раскрывает цепочку
  вложенных импортов BOM-ов;
- `io.github.openfeign:feign-bom:10.12` — тоже явно, иначе feign уехал бы в 13.x;
- gson форсится на **2.8.6**: eureka-client требует `GsonBuilder.setLenient`,
  которого нет в gson 2.1 из Hoxton;
- компиляция с `-parameters` — имена параметров нужны MapStruct (конструкторный
  маппинг immutable-классов) и Spring MVC (`@RequestParam`).

Упаковка — **classpath-дистрибутив** (`appDist`, привязан к `assemble`): jar'ы
зависимостей и jar модуля складываются в `build/dist/lib`, запуск
`java -cp "lib/*" ru.itmo.soa.<app>.Application`. Fat-jar (shadow) запрещён:
при склейке нескольких jar'ов `META-INF/spring.factories` мержится по правилу
`java.util.Properties` (при дублирующихся ключах остаётся последнее значение) —
автоконфигурации Zuul/Eureka молча терялись.

## Конфигурация (Config Server + env)

Механика Boot 2.3/Hoxton: подключение к Config Server задаётся не
`spring.config.import`, а **bootstrap-контекстом** — `bootstrap.yml` в каждом
клиенте: `spring.application.name`, `spring.cloud.config.uri: ${CONFIG_URL}`,
`fail-fast: false` (без config-server сервис обязан стартовать — локальная
разработка).

Приоритеты — ключевая особенность Hoxton: флаг `spring.cloud.config.override-none`
клиент читает **из remote-источников**, поэтому он задан на стороне сервера в
`config-repo/application.yml` (`override-none: true`). С ним remote PropertySource
получает **низший** приоритет — локальные `application*.yml` и dev-профиль (H2)
клиента всегда выигрывают.

Следствие — **грабля с портом oscar-2**: значения в `config-repo/*.yml` — плейсхолдеры
на env (`server.port: ${OSCAR_PORT:8081}`), но remote-источник из-за override-none
проигрывает локальному `application.yml`, и env-значение через remote не
пробрасывается (oscar-2 упорно слушал бы дефолт 8081). Поэтому `server.port`
(и datasource, и параметры movie-service) продублированы в **локальном**
`application.yml` сервиса как плейсхолдеры на те же env-переменные — env имеет
высший приоритет и выигрывает у любого yml.

Разделение конфигурации:
- **Локально** в сервисе: `bootstrap.yml` (имя, адрес Config Server),
  `application.yml` (dev-профиль H2 + env-плейсхолдеры: порт, datasource,
  movie.service.*).
- **Config Server** (native-профиль, `config-repo/`) — внешняя (прод-like) часть:
  те же значения env-плейсхолдерами, `context-path`, CORS, адрес Eureka, JPA.

Настраиваемое живёт в `deploy/.env` (шаблон — `deploy/.env.example`):

| Переменная | Где | По умолчанию |
|-----------|-----|--------------|
| `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL | `soa` |
| `KEYSTORE_PASSWORD` | сертификаты (gateway.p12, truststore) | `changeit` |
| `GATEWAY_PORT` / `FRONTEND_PORT` / `EUREKA_PORT` / `CONFIG_PORT` / `HAPROXY_STATS_PORT` | публикация портов на хост | `8443` / `8444` / `8761` / `8888` / `8404` |
| `CORS_ALLOWED_ORIGINS` | оба сервиса | `https://localhost:8444` |
| `CONFIG_JAVA_OPTIONS` / `EUREKA_JAVA_OPTIONS` / `MOVIE_JAVA_OPTIONS` / `OSCAR_JAVA_OPTIONS` / `GATEWAY_JAVA_OPTIONS` | JVM через `_JAVA_OPTIONS` (согласованы с mem_limit) | см. `.env.example` |

Per-instance значения (задаются в `deploy/docker-compose.yml`): `CONFIG_URL` /
`EUREKA_URL` (in-docker адреса), `MOVIE_PORT` / `OSCAR_PORT` (`8080` / `8081`/`8082`),
`INSTANCE_ID` (`1`/`2` — различает инстансы в реестре Eureka и в заголовке
`X-Instance`), `MOVIE_DB_*` / `OSCAR_DB_*`, `MOVIE_SERVICE_URL`
(`https://gateway:8443/movie-service`), `MOVIE_SERVICE_TRUSTSTORE`
(`/certs/gateway-truststore.p12` + `KEYSTORE_PASSWORD`),
`MOVIE_HUMILIATE_BATCH_SIZE` (`10` — размер чанка режиссёров в батчевом humiliate).

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

## Структура модуля (DDD)

У обоих сервисов единый DDD-слой (пакет `ru.itmo.soa.<service>`):

```
api/          # сгенерированные из OpenAPI интерфейсы (*Api)
dto/          # сгенерированные DTO (javax Bean Validation)
controller/   # @RestController implements *Api; mapper/ (DTO↔domain)
domain/       # чистая доменная модель (immutable-классы/enum), без зависимостей
repository/
  entity/     # JPA-сущности (Hibernate 5)
  jpa/        # JpaRepository + Criteria API / HQL
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
- В `movie-service` координаты и режиссёр — отдельные таблицы, фильм ссылается
  на них через `@ManyToOne`; координаты переиспользуются по значению и ведут
  счётчик ссылок `ref_count` (при 0 — удаляются).

## Сборка и запуск

Всё управляется корневым **Makefile**:

```bash
make build    # appDist всех пяти модулей (classpath-дистрибутивы) + сборка фронтенда (Vite)
make certs    # self-signed сертификаты: gateway.p12, gateway-truststore.p12, frontend.crt/.key
make env      # создать deploy/.env из шаблона (идемпотентно)
make up       # env + проверка certs + docker compose up -d --build
make test     # ./gradlew test (юнит-тесты)
make test-infra   # pytest-тесты против поднятого стека (tests/infra/)
make logs     # docker compose logs -f
make restart  # перезапуск контейнеров
make down     # остановить стек
make clean    # gradle clean + down (без удаления volume)
make purge    # ДЕСТРУКТИВНО: down -v (удалить volume БД) + удалить сертификаты
```

`make up` поднимает `deploy/docker-compose.yml`: postgres → config-server +
eureka-server → movie + oscar-1 + oscar-2 → haproxy → gateway → frontend
(healthchecks и `depends_on` задают порядок старта; все Java-образы — JRE 11).
Конфиги из `config-repo/` монтируются в config-server read-only — правки
конфигурации не требуют пересборки образа.

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
| Config Service | `config-server` (native, `config-repo/`), подключение сервисов через `bootstrap.yml` (Hoxton) |
| Service Discovery (Eureka) | `eureka-server` :8761; регистрация всех сервисов, instance-id с `INSTANCE_ID` |
| Client-side Load Balancer (Ribbon) | Ribbon в api-gateway резолвит `movie-service` через Eureka |
| API Gateway (Zuul) | `api-gateway` :8443 (единый стек Boot 2.3/Hoxton — последний стек с Zuul+Ribbon), единая точка входа HTTPS |
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

- [docs/architecture-hoxton.md](docs/architecture-hoxton.md) — как устроен стек:
  маршруты запросов, Config/Eureka/Feign, единый стек Boot 2.3, грабли Hoxton,
  развёртывание.
- [docs/plan-lab3.md](docs/plan-lab3.md) — исходный план реализации (вариант B).
