# SOA Lab — Movie Collection Service + Oscar Service

Лабораторная работа №2: реализация двух веб-сервисов (Spring Boot, DDD) по OpenAPI-спецификациям
и клиентской документации. Первый сервис управляет коллекцией фильмов, второй добавляет
операции над «Оскарами» и вызывает первый сервис по REST.

## Архитектура

Мультимодульный Gradle-проект (Kotlin DSL). Корневой `build.gradle.kts` задаёт общую
конфигурацию; у каждого сервиса свой модуль и свой `build.gradle.kts`.

| Модуль | Назначение | Упаковка | Порт |
|--------|-----------|----------|------|
| `movie-service` | CRUD + поиск/сортировка/пагинация коллекции `Movie` | **WAR** `movie-service.war` (Payara Server 6) | 8080, context-path `/movie-service` |
| `oscar-service` | Операции с «Оскарами»; вызывает REST API movie-service | **WAR** `oscar.war` (Tomcat 10.1) | 8081, context-path `/oscar` |

### Стек

- Java 21, Spring Boot 3.5, Spring Data JPA, Spring MVC REST
- **OpenAPI codegen** (`org.openapi.generator`): из `openapi/*.yaml` генерируются
  интерфейсы контроллеров и DTO (jakarta, Bean Validation)
- **MapStruct** — маппинг между слоями (DTO↔domain, entity↔domain), **Lombok**
- **Spring Cloud OpenFeign** — вызов movie-service из oscar-service (клиент генерируется из `openapi/movie-service.yaml`)
- **H2** (dev-профиль) / **PostgreSQL** (prod, через env)

## Структура модуля (DDD)

У каждого сервиса единый DDD-слой (пакет `ru.itmo.soa.<service>`):

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

**movie-service** (корень `/`):
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
502/503/504 (ошибки вызова movie-service).

## Конфигурация окружения

Значения задаются переменными окружения (шаблон — `.env.example`, реальный `.env` в gitignore):

| Переменная | Где | По умолчанию |
|-----------|-----|--------------|
| `MOVIE_DB_URL` / `MOVIE_DB_USERNAME` / `MOVIE_DB_PASSWORD` | movie-service | `jdbc:postgresql://localhost:5432/movie` |
| `OSCAR_DB_URL` / `OSCAR_DB_USERNAME` / `OSCAR_DB_PASSWORD` | oscar-service | `jdbc:postgresql://localhost:5432/oscar` |
| `MOVIE_SERVICE_URL` | oscar-service | `http://localhost:8080` (в контейнерах — `https://movie:8443/movie-service`) |
| `MOVIE_HUMILIATE_BATCH_SIZE` | oscar-service | `10` (размер чанка режиссёров в батчевом humiliate) |
| `MOVIE_SERVICE_TRUSTSTORE` / `MOVIE_SERVICE_TRUSTSTORE_PASSWORD` | oscar-service | не задан (Feign-клиент доверяет только JDK cacerts) |
| `CORS_ALLOWED_ORIGINS` | оба | не задан (CORS выключен) |
| `JPA_DDL_AUTO` | оба | `update` |

## Сборка и запуск

```bash
# Сборка всех модулей (+ тесты)
./gradlew build

# movie-service — локально (dev, H2 in-memory)
./gradlew :movie-service:bootRun --args='--spring.profiles.active=dev'

# movie-service — как WAR на Payara Micro (скачивает payara-micro.jar и деплоит)
./gradlew :movie-service:runPayara

# movie-service — WAR для Payara (build/libs/movie-service.war)
./gradlew :movie-service:bootWar

# oscar-service — локально (dev, H2; использует MOVIE_SERVICE_URL)
./gradlew :oscar-service:bootRun --args='--spring.profiles.active=dev'

# oscar-service — WAR для Tomcat (build/libs/oscar.war)
./gradlew :oscar-service:bootWar
```

## Развёртывание в контейнерах

`deploy/docker-compose.yml` поднимает Postgres, movie-service на Payara Server 6.2025.4 (JDK 21)
и oscar-service на Tomcat 10.1 (JDK 21). Перед запуском собираются оба WAR:

```bash
./deploy/gen-certs.sh
./gradlew :movie-service:bootWar :oscar-service:bootWar
cp deploy/.env.example deploy/.env
docker compose -f deploy/docker-compose.yml up -d --build
```

`gen-certs.sh` создаёт в `deploy/certs/` (в gitignore) самоподписанные сертификаты: `movie.p12`, `oscar.p12`,
`movie-truststore.p12` (сертификат movie-service для Feign-клиента oscar-service) и `frontend.crt/.key`.
Каталог монтируется в контейнеры как `/certs`; Payara импортирует `movie.p12` в keystore домена при старте.

С сертификатом Let's Encrypt (на сервере с доменом):

```bash
certbot certonly --standalone -d 64-188-64-15.sslip.io
DOMAIN=64-188-64-15.sslip.io ./deploy/gen-certs.sh
```

и в `deploy/.env`:

```
MOVIE_SERVICE_URL=https://64-188-64-15.sslip.io:8443/movie-service
MOVIE_SERVICE_TRUSTSTORE=
CORS_ALLOWED_ORIGINS=https://64-188-64-15.sslip.io:4173
```

Пустой `MOVIE_SERVICE_TRUSTSTORE` — Feign-клиент доверяет стандартному cacerts JDK. После продления
сертификата повторить `gen-certs.sh` и `docker compose restart movie oscar`.

Оба сервиса доступны только по HTTPS: movie-service — `https://localhost:8443/movie-service`
(у Payara выключен `http-listener-1`, `http-listener-2` переведён на 8443 с сертификатом `movie`),
oscar-service — `https://localhost:8444/oscar` (в `deploy/tomcat/server.xml` только TLS-коннектор).

Порты на хосте задаются в `deploy/.env` (`MOVIE_PORT`, `OSCAR_PORT`), пароль БД — `DB_PASSWORD`,
пароль keystore — `KEYSTORE_PASSWORD`, адрес фронта для CORS — `CORS_ALLOWED_ORIGINS`.

Фронт по HTTPS: `cp frontend/.env.production.example frontend/.env.production`, указать адреса API, затем
`npm --prefix frontend run build && npm --prefix frontend run preview` → `https://localhost:4173`.
В браузере нужно один раз открыть адреса обоих сервисов и принять самоподписанные сертификаты.

Особенности Payara:
- нужен образ с JDK 21 (`-jdk21`): образ по умолчанию работает на Java 11, а сервис собран под 21;
- в WAR лежит `WEB-INF/beans.xml` (`bean-discovery-mode="annotated"`): Payara подключает к каждому
  приложению JSF-листенер, которому нужен CDI, и без `beans.xml` деплой падает с `CDI is not available`.

Запуск oscar-service с полноценным humiliate требует поднятого movie-service.
Данные операторов/оскаров в oscar-service сидируются автоматически при пустых таблицах.

## Документация API (Swagger UI)

Интерактивная документация (из спецификаций) — в каталоге `docs/` (Swagger UI с
переключателем между сервисами). Локально:

```bash
python3 -m http.server 8080
# открыть http://localhost:8080/docs/
```