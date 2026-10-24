# Лаба №3 — как устроен Spring Cloud-стек

Интерактивная архитектурная схема: [docs/architecture-lab3.html](architecture-lab3.html)
(открыть в браузере; тёмная/светлая тема, анимация маршрутов).

## Обзор

Оба сервиса лабы №2 переработаны под микросервисную архитектуру Spring Cloud. Стек целиком
поднимается в docker-compose (`make up`), управление — через `Makefile`.

| Компонент | Роль | Технология | Порт |
|---|---|---|---|
| **config-server** | Config Service — централизованная конфигурация | Spring Cloud Config (native, `config-repo/`) | 8888 |
| **eureka-server** | Service Discovery — реестр сервисов | Netflix Eureka | 8761 |
| **api-gateway** | API Gateway — единая публичная точка входа | **Zuul Proxy + Ribbon** (Spring Cloud Hoxton, Boot 2.3, Java 11) | 8443 (TLS) |
| **movie-service** | «Вызываемый» сервис | Spring Boot 3.5, исполняемый JAR | 8080 (внутр.) |
| **oscar-service ×2** | «Вызывающий» сервис, 2 экземпляра | Spring Boot 3.5, исполняемые JAR | 8081/8082 (внутр.) |
| **haproxy** | Балансировка экземпляров oscar | HAProxy, roundrobin | 9090 (внутр.), stats 8404 |
| **frontend** | SPA + reverse proxy | nginx, отдаёт `frontend/dist`, проксирует API на gateway | 8444 (TLS) |
| **postgres** | Хранилище movie + oscar | PostgreSQL 16 | 5432 (внутр.) |

Единственные публичные HTTPS-точки — frontend (:8444) и gateway (:8443). Внутренний трафик
между контейнерами идёт по plain HTTP.

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
  movie-service слушает с context-path `/movie-service`.
- `/oscar/**` — url-маршрут на `http://haproxy:9090/oscar`; Zuul срезает префикс `/oscar`,
  а URL сам оканчивается на context-path. Дальше HAProxy roundrobin выбирает oscar-1/8081
  или oscar-2/8082 (инстанс помечает свои ответы заголовком `X-Instance: 1|2`).

### Межсервисный вызов (oscar → movie)
По заданию все запросы к сервисам идут через gateway: Feign-клиент oscar настроен на
`MOVIE_SERVICE_URL=https://gateway:8443/movie-service`. TLS gateway — self-signed, поэтому
oscar доверяет ему через `MOVIE_SERVICE_TRUSTSTORE=/certs/gateway-truststore.p12`
(сертификат gateway импортирован в truststore при `make certs`).

Цепочка: `oscar → (Feign, TLS, truststore) → gateway → (Ribbon/Eureka) → movie`.

## Конфигурация (Config Service)

- `config-server/src/main/resources/config-repo/` — источник конфигов (нативный профиль,
  монтируется в контейнер read-only):
  - `application.yml` — общее: JPA, `EUREKA_URL` для eureka-клиента;
  - `movie-service.yml` / `oscar-service.yml` — порты, datasource, context-path, CORS и т.д.
- Локальный `application.yml` каждого сервиса содержит только `spring.application.name`
  и `spring.config.import: optional:configserver:${CONFIG_URL:http://localhost:8888}`.
  `optional:` позволяет запускаться без config-server (локальный dev-профиль с H2 работает как раньше).
- Приоритет: удалённый PropertySource **ниже** локального файла, поэтому всё переопределяемое
  живёт в config-repo, а локально — только bootstrap.

## Service Discovery (Eureka)

При старте каждый сервис (movie, oscar-1, oscar-2, gateway) регистрируется в Eureka
(`EUREKA_URL=http://eureka:8761/eureka`) и шлёт heartbeats каждые 30с. Gateway обновляет
реестр каждые 5с (`registry-fetch-interval-seconds`), self-preservation выключена —
«умершие» инстансы выпадают быстро. Идентификаторы инстансов oscar: `oscar-service:1`
и `oscar-service:2` (`INSTANCE_ID`), по ним же балансировка проверяется в тестах.

## Legacy-остров api-gateway

Настоящие Zuul Proxy и Ribbon существуют только в Spring Cloud **Hoxton** (Spring Boot 2.3,
Java 8–14) — в современных версиях они удалены. Поэтому gateway — отдельный модуль на
Boot 2.3/Java 11, изолированный от корневого стека Boot 3.5/Java 21:

- gradle-плагин Boot 2.x несовместим с Gradle 8 → версии пинятся BOM-ом
  `spring-cloud-dependencies:Hoxton.SR12`, без плагина;
- **упаковка** — classpath-дистрибутив (`api-gateway/build/dist/lib`, запуск
  `java -cp "lib/*" ru.itmo.soa.gateway.ApiGatewayApplication`): fat-jar (shadow) склеивает
  `META-INF/spring.factories` из десятков jar'ов, а `java.util.Properties` при дублирующихся
  ключах оставляет последнее значение — автоконфигурации Zuul/Eureka молча терялись;
- gson форсится на 2.8.6: eureka-client требует `GsonBuilder.setLenient`, которого нет в
  gson 2.1 из Hoxton.

## Развёртывание и эксплуатация

```bash
make build       # jar'ы 4 модулей + gatewayDist + сборка фронтенда (VITE_*=/movie-service, /oscar)
make certs       # gateway.p12, gateway-truststore.p12, frontend.crt/key (self-signed)
make up          # docker compose up -d --build
make test        # юнит-тесты сервисов (gradle)
make test-infra   # pytest tests/infra/ — проверки поднятого стека
make logs / ps / restart / down / purge   # purge = down -v + удаление certs
```

Порядок старта в compose: postgres → config-server + eureka-server (healthcheck через curl —
в bash базового образа temurin нет /dev/tcp) → movie + oscar-1/2 → haproxy → gateway → frontend.
HAProxy резолвит бекенды через docker-DNS (`resolvers docker`, 127.0.0.11) — при пересоздании
контейнеров IP меняются, статический кэш привёл бы к connection refused.

## Инфраструктурные тесты (`tests/infra/`)

pytest + requests, `make test-infra` (стек должен быть поднят):
1. **test_eureka** — movie и оба инстанса oscar зарегистрированы в статусе UP;
2. **test_config_server** — config-server отдаёт конфиги movie/oscar;
3. **test_gateway** — проксирование через gateway (genres, search, losers) по HTTPS;
4. **test_api_compat** — контракт лабы №2 сохранён через gateway: CRUD фильма
   (201+Location → GET → PUT → DELETE → 404), невалидное тело → 400/422;
5. **test_haproxy** — roundrobin: повторные запросы попадают в оба инстанса (X-Instance).

## Соответствие заданию

| Требование | Чем закрыто |
|---|---|
| Config Service | spring-cloud-config-server, native `config-repo/` |
| Service Discovery (Eureka) | eureka-server; все сервисы — eureka-клиенты |
| Load Balancer (Ribbon) | Ribbon в Zuul-маршрутах gateway (service-id маршруты) |
| API Gateway (Zuul Proxy) | api-gateway: `@EnableZuulProxy`, публичный HTTPS :8443 |
| Второй экземпляр вызывающего сервиса + Haproxy | oscar-1 + oscar-2 за HAProxy (roundrobin) |
| Полная совместимость с API лабы №2 | пути `/movie-service/**`, `/oscar/**` и контракты не изменены; покрыто test_api_compat |
