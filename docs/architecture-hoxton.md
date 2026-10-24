# Лаба №3 — как устроен Spring Cloud-стек (full-Hoxton)

Ветка `lab3-hoxton`: **единый стек Spring Boot 2.3.12 / Spring Cloud Hoxton.SR12 / Java 11**
для всех модулей — сервисов, инфраструктуры и gateway.

## Обзор

Оба сервиса лабы №2 переведены на Boot 2.3 (javax, Hibernate 5) и переработаны под
микросервисную архитектуру Spring Cloud. Стек целиком поднимается в docker-compose
(`make up`), управление — через `Makefile`.

| Компонент | Роль | Технология | Порт |
|---|---|---|---|
| **config-server** | Config Service — централизованная конфигурация | Spring Cloud Config (native, `config-repo/`) | 8888 |
| **eureka-server** | Service Discovery — реестр сервисов | Netflix Eureka | 8761 |
| **api-gateway** | API Gateway — единая публичная точка входа | **Zuul Proxy + Ribbon** (единый стек Boot 2.3, Java 11) | 8443 (TLS) |
| **movie-service** | «Вызываемый» сервис | Spring Boot 2.3, classpath-дистрибутив | 8080 (внутр.) |
| **oscar-service ×2** | «Вызывающий» сервис, 2 экземпляра | Spring Boot 2.3, classpath-дистрибутивы | 8081/8082 (внутр.) |
| **haproxy** | Балансировка экземпляров oscar | HAProxy, roundrobin | 9090 (внутр.), stats 8404 |
| **frontend** | SPA + reverse proxy | nginx, отдаёт `frontend/dist`, проксирует API на gateway | 8444 (TLS) |
| **postgres** | Хранилище movie + oscar | PostgreSQL 16 | 5432 (внутр.) |

Единственные публичные HTTPS-точки — frontend (:8444) и gateway (:8443). Внутренний
трафик между контейнерами идёт по plain HTTP.

## Маршруты запросов

### Запрос пользователя
```
Browser → https://host:8444 (nginx)
   ├─ статика: SPA (frontend/dist)
   └─ /movie-service/**, /oscar/** → proxy_pass https://gateway:8443 (TLS, self-signed)
```

### Через Zuul (gateway)
- `/movie-service/**` — Ribbon-маршрут: Zuul спрашивает у Eureka инстансы `MOVIE-SERVICE`
  и балансирует между ними (Ribbon). `strip-prefix: false` — путь уходит целиком, т.к.
  movie-service слушает с context-path `/movie-service`. Ribbon-таймауты подняты
  (`ReadTimeout: 10000`) — батч-вызов humiliate может быть медленным.
- `/oscar/**` — url-маршрут на `${OSCAR_URL:http://haproxy:9090/oscar}`; Zuul срезает
  префикс `/oscar`, а URL сам оканчивается на context-path. Дальше HAProxy roundrobin
  выбирает oscar-1/8081 или oscar-2/8082 (инстанс помечает свои ответы заголовком
  `X-Instance: 1|2`).

### Межсервисный вызов (oscar → movie)
По заданию все запросы к сервисам идут через gateway: Feign-клиент oscar настроен на
`MOVIE_SERVICE_URL=https://gateway:8443/movie-service`. TLS gateway — self-signed,
поэтому oscar доверяет ему через `MOVIE_SERVICE_TRUSTSTORE=/certs/gateway-truststore.p12`
(сертификат gateway импортирован в truststore при `make certs`).

Цепочка: `oscar → (Feign, TLS, truststore) → gateway → (Ribbon/Eureka) → movie`.

## Единый стек Boot 2.3 / Hoxton

Настоящие Zuul Proxy и Ribbon существуют только в Spring Cloud **Hoxton** (Spring
Boot 2.3, Java 8–14) — в современных версиях они удалены. Вместо «modern-стека +
legacy-остров» проект переведён на Hoxton **целиком**: одна платформа для всех
пяти Java-модулей, один набор граблей, единые BOM-ы в корневом билде.

Отличия от стека Boot 3.x, с которыми пришлось считаться:

- **javax вместо jakarta** — сгенерированные OpenAPI-интерфейсы/DTO и весь Spring
  MVC/JPA используют `javax.*`; Hibernate 5, а не 6.
- **Hibernate 5: нет неявных join'ов в bulk-update HQL** — `UPDATE ... WHERE
  m.director.name IN ...` сгенерировал бы невалидный cross join. В
  `MovieJpaRepository.nullOscarsByDirectors` режиссёры выбираются подзапросом:
  `m.director.id IN (SELECT p.id FROM PersonEntity p WHERE p.name IN :directors)`
  — семантика та же, что в Hibernate 6.
- **OpenFeign 10.12 — одноуровневое наследование**: `@FeignClient`-интерфейс не может
  наследовать методы у родителя, а сгенерированный `MoviesApi` сам наследует
  `ApiClient.Api`. Поэтому `MovieServiceFeignClient` написан вручную: методы
  переобъявлены с нативными аннотациями `@RequestLine`/`@Headers` поверх тех же
  сгенерированных DTO; контракт — `feign.Contract.Default` (`MovieFeignConfiguration`).
- **@MockBean вместо @MockitoBean** в тестах контроллеров (Boot 2.3 Spring Test).
- **gson 2.8.6 force**: eureka-client требует `GsonBuilder.setLenient`, которого
  нет в gson 2.1, пинимой Hoxton.

## Грабли Hoxton (найдены и закрытые)

1. **Bootstrap вместо config-import.** В Boot 2.3 нет `spring.config.import`:
   имя приложения и адрес Config Server задаются в `bootstrap.yml` каждого клиента
   (`spring.cloud.config.uri: ${CONFIG_URL}`, `fail-fast: false` — без config-server
   сервис обязан стартовать для локальной разработки).
2. **override-none — на стороне сервера.** Флаги приоритета
   (`PropertySourceBootstrapConfiguration`) bind'ятся в Hoxton **из самих
   remote-источников**, не из bootstrap.yml клиента: сервер решает, может ли клиент
   перекрывать remote-значения. Поэтому `spring.cloud.config.override-none: true`
   задан в `config-repo/application.yml`. С ним remote PropertySource — дефолт с
   **низшим** приоритетом: локальные `application*.yml` и dev-профиль клиента
   выигрывают (иначе postgres-URL из config-repo перебивал бы H2 из dev-профиля).
3. **server.port через локальный env-плейсхолдер.** Значения в config-repo —
   плейсхолдеры на env (`server.port: ${OSCAR_PORT:8081}`), но при override-none
   remote-источник проигрывает локальному yml: env-значение, «запечённое» в remote,
   до свойства не доходит. Грабля проявилась на oscar-2: инстанс игнорировал
   `OSCAR_PORT=8082` и слушал дефолт 8081 (конфликт портов с oscar-1). Решение:
   `server.port` (и datasource, и параметры movie.service) продублированы в
   **локальном** `application.yml` сервиса как плейсхолдеры на те же env —
   env имеет высший приоритет и выигрывает у любого yml.
4. **Feign 10.12 и gson — явные BOM/force** (см. выше разделы): плагин
   dependency-management не раскрывает цепочку вложенных импортов BOM-ов, поэтому
   `spring-boot-dependencies:2.3.12` и `feign-bom:10.12` импортированы явно.

## Конфигурация (Config Service)

- `config-server/src/main/resources/config-repo/` — источник конфигов (нативный
  профиль, монтируется в контейнер read-only):
  - `application.yml` — общее: `override-none: true`, JPA, `EUREKA_URL`,
    `prefer-ip-address`;
  - `movie-service.yml` / `oscar-service.yml` — порты, datasource, context-path,
    параметры movie-service и CORS (env-плейсхолдерами).
- Локальный `application.yml` сервиса — dev-профиль (H2) и env-плейсхолдеры
  значений, критичных per-instance (порт, datasource, movie.service.*).
  `bootstrap.yml` — имя и адрес Config Server.

## Service Discovery (Eureka)

При старте каждый сервис (movie, oscar-1, oscar-2, gateway) регистрируется в Eureka
(`EUREKA_URL=http://eureka:8761/eureka`) и шлёт heartbeats каждые 30с. Gateway
обновляет реестр каждые 5с (`registry-fetch-interval-seconds`). Идентификаторы
инстансов oscar: `oscar-service:1` и `oscar-service:2` (`INSTANCE_ID`), по ним же
балансировка проверяется в тестах; свои ответы инстанс помечает `X-Instance`.

## Сборка и упаковка (classpath-dist)

Градle-плагин Spring Boot 2.x несовместим с Gradle 8 (LazyPublishArtifact
NoSuchMethodError) → плагин Boot не применяется нигде, версии пинятся BOM-ами
(`spring-boot-dependencies:2.3.12.RELEASE`, `spring-cloud-dependencies:Hoxton.SR12`,
`feign-bom:10.12` — два последних импортированы явно). Компиляция с `-parameters`
(MapStruct-конструкторный маппинг, `@RequestParam`).

Упаковка каждого модуля — classpath-дистрибутив `appDist` (задача `Sync` в корневом
билде, привязана к `assemble`): `build/dist/lib` со всеми jar'ами зависимостей +
jar модуля, запуск `java -cp "lib/*" MainClass`. Fat-jar (shadow) запрещён:
`META-INF/spring.factories` при склейке мержится по правилу `java.util.Properties`
(дублирующиеся ключи → последнее значение), автоконфигурации Zuul/Eureka молча
терялись.

## Развёртывание и эксплуатация

```bash
make build       # appDist всех пяти модулей + сборка фронтенда (VITE_*=/movie-service, /oscar)
make certs       # gateway.p12, gateway-truststore.p12, frontend.crt/key (self-signed)
make env         # deploy/.env из шаблона .env.example
make up          # docker compose up -d --build (проверяет наличие certs)
make test        # юнит-тесты сервисов (gradle)
make test-infra   # pytest tests/infra/ — проверки поднятого стека
make logs / ps / restart / down / clean / purge   # purge = down -v + удаление certs
```

Порядок старта в compose: postgres → config-server + eureka-server (healthcheck
через curl — в образе JRE нет /dev/tcp) → movie + oscar-1/2 → haproxy → gateway →
frontend. Все Java-образы — JRE 11; JVM-опции (`*_JAVA_OPTIONS` через
`_JAVA_OPTIONS`) согласованы с `mem_limit` контейнеров.

## Инфраструктурные тесты (`tests/infra/`)

pytest + requests, `make test-infra` (стек должен быть поднят):
1. **test_eureka** — movie и оба инстанса oscar зарегистрированы в статусе UP;
2. **test_config_server** — config-server отдаёт конфиги movie/oscar;
3. **test_gateway** — проксирование через gateway (genres, search, losers) по HTTPS;
4. **test_api_compat** — контракт лабы №2 сохранён через gateway: CRUD фильма
   (201+Location → GET → PUT → DELETE → 404), невалидное тело → 400/422;
5. **test_haproxy** — roundrobin: повторные запросы попадают в оба инстанса
   (X-Instance).

## Соответствие заданию

| Требование | Чем закрыто |
|---|---|
| Config Service | spring-cloud-config-server, native `config-repo/`; клиенты через `bootstrap.yml` |
| Service Discovery (Eureka) | eureka-server; все сервисы — eureka-клиенты |
| Load Balancer (Ribbon) | Ribbon в Zuul-маршрутах gateway (service-id маршруты) |
| API Gateway (Zuul Proxy) | api-gateway: `@EnableZuulProxy`, публичный HTTPS :8443; Zuul+Ribbon есть только в Hoxton — потому весь стек на Boot 2.3 |
| Второй экземпляр вызывающего сервиса + Haproxy | oscar-1 + oscar-2 за HAProxy (roundrobin), различимы по `X-Instance` |
| Полная совместимость с API лабы №2 | пути `/movie-service/**`, `/oscar/**` и контракты не изменены; покрыто test_api_compat |
