# Лаба №3 — План A: «Legacy gateway-остров» (сервисы остаются на Boot 3.5)

Цель: перевести оба сервиса на платформу Spring Cloud (Eureka, Config Service, Load Balancer/Ribbon),
все внешние запросы — через API Gateway (Zuul Proxy), два экземпляра oscar-service за HAProxy.
Существующие movie-service и oscar-service остаются на Spring Boot 3.5 / Java 21 — не трогаем их код,
кроме упаковки и конфигурации Spring Cloud.

## Архитектура (docker)

```
Интернет → api-gateway (Zuul, HTTPS :8443, Boot 2.3/Hoxton/Java 11)
             ├─ /movie-service/** → Ribbon → Eureka → movie-service (Boot 3.5)
             └─ /oscar/**        → haproxy (roundrobin) → oscar-1, oscar-2 (Boot 3.5)
frontend (nginx, :8444) → gateway
config-server (Boot 3.5, native FS ← config-repo/) ← все сервисы
eureka-server (Boot 3.5, :8761) ← все регистрируются
postgres
```

- Feign-клиент oscar (`MOVIE_SERVICE_URL`) направлен на gateway — межсервисные вызовы тоже
  проксируются через Zuul (требование задания).
- Внешний HTTPS: gateway :8443, фронт :8444 (самоподписанные сертификаты, `gen-certs.sh`).
- Внутренний трафик между контейнерами — plain HTTP; у Feign остаётся существующий механизм
  `MOVIE_SERVICE_TRUSTSTORE` для сертификата gateway.

## Новые модули Gradle (структура проекта сохраняется, только добавления)

1. **`config-server`** — Boot 3.5, `spring-cloud-config-server`, native-профиль.
   Конфиги в `config-repo/`: `application.yml`, `movie-service.yml`, `oscar-service.yml`
   (все существующие env-плейсхолдеры без изменений).
2. **`eureka-server`** — Boot 3.5, `spring-cloud-starter-netflix-eureka-server`, порт 8761, standalone.
3. **`api-gateway`** — legacy-остров: Spring Boot 2.3 + Spring Cloud **Hoxton** + toolchain Java 11
   (последняя линейка с настоящими Zuul и Ribbon).
   - `@EnableZuulProxy`, Ribbon-маршруты по service-id из Eureka;
   - маршрут `/oscar` закреплён на хост `haproxy` (балансировку между инстансами oscar делает HAProxy);
   - собственный `build.gradle.kts`, исключённый из корневого `subprojects{}` (условный apply);
   - Dockerfile на `eclipse-temurin:11-jre`.

## Изменения существующих сервисов (минимальные)

- Оба: WAR → исполняемый JAR (`bootJar`; убрать Payara/Tomcat-специфику и `providedRuntime`);
  добавить `spring-cloud-starter-config` (spring.config.import) и
  `spring-cloud-starter-netflix-eureka-client`; env-переменные (`MOVIE_DB_URL` и т.д.) сохраняются.
- **movie-service**: внутренний порт 8080, context-path `/movie-service` остаётся.
- **oscar-service**: Feign `MOVIE_SERVICE_URL` → gateway; два инстанса в compose (:8081/:8082),
  одно имя приложения, одна БД.

## Docker / env / Makefile

- `deploy/docker-compose.yml` — новый стек: postgres, config-server, eureka-server, movie,
  oscar-1, oscar-2, haproxy, api-gateway, frontend(nginx); healthchecks и порядок старта:
  config → eureka → сервисы → haproxy/gateway.
- `deploy/haproxy/haproxy.cfg` — roundrobin на oscar-1:8081 / oscar-2:8082, stats-страница.
- `deploy/.env` — существующие переменные + `GATEWAY_PORT`, `EUREKA_PORT`, `CONFIG_PORT`,
  порты инстансов oscar; `gen-certs.sh` адаптировать (сертификат gateway для Feign truststore).
- **Makefile** в корне: `build` (gradle bootJar ×4 + фронт), `certs`, `up`, `down`, `logs`,
  `restart`, `test-infra`, `clean`.

## Инфраструктурные тесты (python, `tests/infra/`)

pytest + requests; `conftest.py` — эндпоинты из `.env`, session-фикстуры с ожиданием готовности стека.

1. Eureka: `/eureka/apps` содержит инстансы MOVIE-SERVICE и OSCAR-SERVICE в статусе UP.
2. Config Server: `/{movie,oscar}-service/<profile>` отдаёт конфиг.
3. Проксирование gateway: `GET /movie-service/movies/genres`, `GET /oscar/operators/losers` по HTTPS.
4. Совместимость API лабы №2 через gateway: CRUD фильма (201+Location, 404, 422), сценарий humiliate.
5. Балансировка HAProxy: заголовок `X-Instance` (добавить в конфиг oscar) → повторные запросы
   попадают в оба инстанса; stop одного контейнера → сервис продолжает отвечать (health-check).
6. Межсервисный вызов через gateway: эндпоинты oscar, зовущие movie (losers, humiliate), работают.

Запуск: `make test-infra` (проверка самоподписанного сертификата отключена только в тестах).

## Верификация

1. `make build && make up` — `docker compose ps` всё green.
2. `make test-infra` — все pytest-наборы зелёные.
3. `./gradlew build` — юнит-тесты сервисов не тронуты и зелёные.
4. Вручную: фронт и Swagger UI через адрес gateway (VITE_* переключены).

## Риски

- EOL-артефакты Hoxton при сборке legacy-модуля; риск локализован в одном новом модуле
  `api-gateway` и не затрагивает существующие сервисы.
- Fallback (задокументирован, не входит в план): Spring Cloud Gateway + Spring Cloud LoadBalancer.
