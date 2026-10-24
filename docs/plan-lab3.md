# Лаба №3 — План B: «Всё на Hoxton» (полный откат проекта на Boot 2.3)

Цель: та же топология микросервисов (Eureka, Config Service, Zuul + Ribbon, HAProxy), но ВСЕ модули
проекта переводятся на одну легаси-линейку: Spring Boot 2.3 + Spring Cloud Hoxton + Java 11.
Zuul и Ribbon становятся «родными» для всей платформы, ценой миграции существующего кода.

## Архитектура (docker)

```
Интернет → api-gateway (Zuul Proxy, HTTPS :8443)
             ├─ /movie-service/** → Ribbon → Eureka → movie-service (Boot 2.3)
             └─ /oscar/**        → haproxy (roundrobin) → oscar-1, oscar-2 (Boot 2.3)
frontend (nginx, :8444) → gateway
config-server (Hoxton, native FS ← config-repo/) ← все сервисы
eureka-server (Hoxton, :8761) ← все регистрируются
postgres
```

Топология и контракты внешнего API — идентичны плану A; отличается только версия платформы
всех модулей и объём миграции кода.

## Новые модули Gradle

- **`config-server`** — Boot 2.3/Hoxton, `spring-cloud-config-server`, native-профиль, `config-repo/`.
- **`eureka-server`** — Boot 2.3/Hoxton, `spring-cloud-starter-netflix-eureka-server`, :8761.
- **`api-gateway`** — Boot 2.3/Hoxton, `@EnableZuulProxy` + Ribbon (service-id маршруты через
  Eureka; `/oscar` — на haproxy). Никакого «острова»: единый корневой `subprojects{}`.

## Этапы миграции существующего кода

1. **Корневые скрипты сборки**: `build.gradle.kts` — Spring Boot 2.3.x, Java 11 toolchain,
   dependency-management Hoxton (SR-линейка: `spring-cloud-dependencies:Hoxton.SR12`);
   `settings.gradle.kts` — добавить три новых модуля.
2. **Java 21 → 11**: переписать все `record` в доменных слоях
   (`movie-service/src/main/java/.../domain/`, `oscar-service/src/main/java/.../domain/`)
   в обычные классы (Lombok/Immutable-стиль по образцу проекта); убрать switch-expressions
   и pattern matching, если встречаются.
3. **Jakarta → javax**:
   - в обоих `build.gradle.kts` OpenAPI-генератор: `useSpringBoot3: false`, `useJakartaEe: false`;
     перегенерация API-интерфейсов и DTO (пакеты `ru.itmo.soa.*.api`, `ru.itmo.soa.*.dto`);
   - контроллеры: `jakarta.validation` → `javax.validation`, `jakarta.servlet` → `javax.servlet`;
   - `generateMovieFeignClient` в oscar — та же перегенерация.
4. **Hibernate 6 → 5 / JPA 2.x**: переработать `repository/jpa/` в movie-service
   (фильтры/сортировка/пагинация на Criteria API — API между версиями различается);
   обновить `JpaMovieDaoTest` и связанные юнит-тесты.
5. **Spring Cloud OpenFeign → Hoxton**: проверить `FeignErrorDecoder`, `Contract.Default`,
   OkHttp-зависимости и конфигурацию клиента (`oscar-service/src/main/java/.../client/`,
   `config/FeignConfig`) под старым spring-cloud-openfeign; поправить при необходимости.
6. **Lombok/MapStruct**: версии, совместимые с Java 11 (текущие 1.6.3 — проверить
   annotation processing на Boot 2.3; при проблемах — даунгрейд MapStruct до 1.4.x).
7. **WAR → JAR**: оба сервиса переводятся на `bootJar`; Payara/Tomcat-специфика удаляется
   (Dockerfile на `eclipse-temurin:11-jre` для всех сервисов).

## Docker / env / Makefile

- `deploy/docker-compose.yml`: postgres, config-server, eureka-server, movie, oscar-1, oscar-2,
  haproxy, api-gateway, frontend(nginx); healthchecks; порядок старта config → eureka → сервисы →
  haproxy/gateway.
- `deploy/haproxy/haproxy.cfg` — roundrobin oscar-1:8081 / oscar-2:8082 + stats.
- `deploy/.env` — существующие переменные + `GATEWAY_PORT`, `EUREKA_PORT`, `CONFIG_PORT`;
  `gen-certs.sh` адаптировать под сертификат gateway (Feign truststore).
- **Makefile** в корне: `build`, `certs`, `up`, `down`, `logs`, `restart`, `test-infra`, `clean`.
- oscar: Feign `MOVIE_SERVICE_URL` → gateway; два инстанса, одно имя приложения, одна БД.

## Инфраструктурные тесты (python, `tests/infra/`)

Идентичны плану A (pytest + requests, `tests/infra/`, `conftest.py` с ожиданием стека):

1. Eureka: инстансы MOVIE-SERVICE и OSCAR-SERVICE UP.
2. Config Server отдаёт конфиги movie/oscar.
3. Проксирование gateway по HTTPS: genres, losers.
4. Совместимость API лабы №2 через gateway: CRUD (201+Location, 404, 422), humiliate.
5. HAProxy: заголовок `X-Instance` → оба инстанса; stop одного контейнера → сервис жив.
6. Межсервисный вызов через gateway (losers/humiliate).

## Верификация

1. `./gradlew build` — все юнит-тесты обоих сервисов зелёные после миграции
   (главный критерий того, что откат не сломал поведение).
2. `make build && make up` — compose green.
3. `make test-infra` — инфраструктурные тесты зелёные.
4. Вручную: фронт и Swagger UI через gateway.

## Риски

- EOL на весь проект: Hoxton/Boot 2.3 не получают патчи безопасности с ~2022 г.
- Максимальный объём правок существующего проверенного кода: records, jakarta→javax,
  Criteria API, Feign — регрессии ловятся только юнит- и инфраструктурными тестами.
- Преимущество: Zuul/Ribbon демонстрируются «нативно» на единой платформе без
  изолированного модуля.
