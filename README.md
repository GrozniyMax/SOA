# SOA Lab — Movie Collection Service + Oscar Service

Лабораторная работа №2: реализация двух веб-сервисов (Spring Boot, DDD) по OpenAPI-спецификациям
и клиентской документации. Первый сервис управляет коллекцией фильмов, второй добавляет
операции над «Оскарами» и вызывает первый сервис по REST.

## Архитектура

Мультимодульный Gradle-проект (Kotlin DSL). Корневой `build.gradle.kts` задаёт общую
конфигурацию; у каждого сервиса свой модуль и свой `build.gradle.kts`.

| Модуль | Назначение | Упаковка | Порт |
|--------|-----------|----------|------|
| `movie-service` | CRUD + поиск/сортировка/пагинация коллекции `Movie` | **WAR** (Payara / Payara Micro) | 8080 |
| `oscar-service` | Операции с «Оскарами»; вызывает REST API movie-service | **JAR** (embedded Tomcat) | 8081, context-path `/oscar` |

### Стек

- Java 21, Spring Boot 3.5, Spring Data JPA, Spring MVC REST
- **OpenAPI codegen** (`org.openapi.generator`): из `openapi/*.yaml` генерируются
  интерфейсы контроллеров и DTO (jakarta, Bean Validation)
- **MapStruct** — маппинг между слоями (DTO↔domain, entity↔domain), **Lombok**
- **WebClient** (WebFlux) — вызов movie-service из oscar-service
- **H2** (dev-профиль) / **PostgreSQL** (prod, через env)

## Структура модуля (DDD)

У каждого сервиса единый DDD-слой (пакет `ru.tbank.soa.<service>`):

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
client/       # (oscar-service) WebClient-клиент к movie-service
config/       # @ConfigurationProperties, WebClient, сидер данных
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
| `MOVIE_SERVICE_URL` | oscar-service | `http://localhost:8080` |
| `JPA_DDL_AUTO` | оба | `update` |

## Сборка и запуск

```bash
# Сборка всех модулей (+ тесты)
./gradlew build

# movie-service — локально (dev, H2 in-memory)
./gradlew :movie-service:bootRun --args='--spring.profiles.active=dev'

# movie-service — как WAR на Payara Micro (скачивает payara-micro.jar и деплоит)
./gradlew :movie-service:runPayara

# movie-service — WAR для внешнего Payara
./gradlew :movie-service:bootWar

# oscar-service — локально (dev, H2; использует MOVIE_SERVICE_URL)
./gradlew :oscar-service:bootRun --args='--spring.profiles.active=dev'

# oscar-service — исполняемый JAR
./gradlew :oscar-service:bootJar
```

Запуск oscar-service с полноценным humiliate требует поднятого movie-service.
Данные операторов/оскаров в oscar-service сидируются автоматически при пустых таблицах.

## Документация API (Swagger UI)

Интерактивная документация (из спецификаций) — в каталоге `docs/` (Swagger UI с
переключателем между сервисами). Локально:

```bash
python3 -m http.server 8080
# открыть http://localhost:8080/docs/
```