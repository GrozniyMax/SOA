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
| **api-gateway** | Маршрутизация запросов к Movie и Oscar | **Zuul Proxy + Ribbon** (Boot 2.3, Java 11) | 8443 (TLS, внутр.) |
| **movie-service** | «Вызываемый» сервис | Spring Boot 2.3, classpath-дистрибутив | 8080 (внутр.) |
| **oscar-service ×2** | «Вызывающий» сервис, 2 экземпляра | Spring Boot 2.3, classpath-дистрибутивы | 8081/8082 (внутр.) |
| **eureka-dns** | DNS-записи для экземпляров Oscar и Gateway из Eureka | DNS SRV, A и AAAA | 8053/TCP и UDP (внутр.) |
| **haproxy** | Балансировка экземпляров Oscar | HAProxy, DNS `server-template`, roundrobin | 9090 (внутр.), stats 8404 |
| **frontend** | SPA и балансировка API между экземплярами Gateway | Nginx, динамический upstream по DNS SRV | 8444 (SPA), 8443 (API) |
| **postgres** | Хранилище movie + oscar | PostgreSQL 16 | 5432 (внутр.) |

Nginx принимает HTTPS на `:8444` и `:8443` и подключается к Gateway по HTTPS.
Gateway обращается к Movie и HAProxy по HTTP.

## Маршруты запросов

### Запрос пользователя
```
Browser → https://host:8444 (nginx)
   ├─ статика: SPA (frontend/dist)
   └─ /movie-service/**, /oscar/** → Gateway из Eureka (HTTPS)
API-клиент → https://host:8443 (nginx) → тот же пул Gateway (HTTPS)
```

### Через Zuul (gateway)
- `/movie-service/**` — Ribbon-маршрут: Zuul спрашивает у Eureka инстансы `MOVIE-SERVICE`
  и балансирует между ними (Ribbon). `strip-prefix: false` — путь уходит целиком, т.к.
  movie-service слушает с context-path `/movie-service`. Ribbon-таймауты подняты
  (`ReadTimeout: 10000`) — батч-вызов humiliate может быть медленным.
- `/oscar/**` — url-маршрут на `${OSCAR_URL:http://haproxy:9090/oscar}`; Zuul срезает
  префикс `/oscar`, а URL сам оканчивается на context-path. HAProxy распределяет
  запросы по round-robin между экземплярами, обнаруженными через DNS. Экземпляры
  `oscar-1:8081` и `oscar-2:8082` помечают ответы заголовком `X-Instance: 1|2`.

### Межсервисный вызов (oscar → movie)
Feign-клиент Oscar обращается к `movie-service` по имени. Spring Cloud LoadBalancer
выбирает экземпляр из локального реестра Eureka-клиента. Oscar обновляет реестр
каждые 5 секунд и отправляет запрос напрямую по HTTP в сети Compose. Префикс
`/movie-service` задаётся через `MOVIE_SERVICE_PATH`.

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

### Обнаружение Gateway и Oscar через DNS

`eureka-dns` опрашивает `OSCAR-SERVICE` и `API-GATEWAY` в Eureka каждые 5 секунд.
Для каждого экземпляра `UP` адаптер публикует запись SRV с портом и запись A
или AAAA с IP-адресом. HAProxy запрашивает `_oscar._tcp.discovery.internal`
по TCP, заполняет до 10 слотов `server-template` и проверяет TCP-порты.
Nginx запрашивает `_gateway._tcp.discovery.internal`, который содержит
HTTPS-порты Gateway. Nginx применяет изменения к upstream без перезапуска.
У адаптера фиксированный адрес в отдельной сети Compose `discovery`, чтобы оба
прокси могли продолжать использовать его после пересоздания контейнера.

Смена статуса на `OUT_OF_SERVICE` убирает экземпляр из DNS после обновления
реестра. При временной ошибке Eureka адаптер использует последний успешный
снимок до 30 секунд, после чего отвечает `SERVFAIL`. Успешный ответ с пустым
списком экземпляров приводит к удалению DNS-записей.

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

Movie и Oscar ждут готовности PostgreSQL, Config Server и Eureka. DNS-адаптер
ждёт готовности Eureka. Nginx и HAProxy запускаются после адаптера и обнаруживают
экземпляры по мере регистрации. Gateway ждёт запуска HAProxy и готовности Eureka.
Java-контейнеры используют JRE 11.

## Инфраструктурные тесты (`tests/infra/`)

pytest + requests, `make test-infra` (стек должен быть поднят):
1. **test_eureka** — movie и оба инстанса oscar зарегистрированы в статусе UP;
2. **test_config_server** — config-server отдаёт конфиги movie/oscar;
3. **test_gateway** — проксирование через gateway (genres, search, losers) по HTTPS;
4. **test_api_compat** — контракт лабы №2 сохранён через gateway: CRUD фильма
   (201+Location → GET → PUT → DELETE → 404), невалидное тело → 400/422;
5. **test_haproxy** — round-robin по `X-Instance`, переключение при остановке
   контейнера и изменение ротации при смене статуса в Eureka.

## Соответствие заданию

| Требование | Чем закрыто |
|---|---|
| Config Service | spring-cloud-config-server, native `config-repo/`; клиенты через `bootstrap.yml` |
| Service Discovery (Eureka) | eureka-server; все сервисы — eureka-клиенты |
| Load Balancer (Ribbon) | Ribbon в Zuul-маршрутах gateway (service-id маршруты) |
| API Gateway (Zuul Proxy) | `@EnableZuulProxy`, внутренний HTTPS `:8443`. Nginx балансирует экземпляры и принимает внешние запросы |
| Второй экземпляр вызывающего сервиса + Haproxy | oscar-1 + oscar-2 за HAProxy (roundrobin), различимы по `X-Instance` |
| Полная совместимость с API лабы №2 | пути `/movie-service/**`, `/oscar/**` и контракты не изменены; покрыто test_api_compat |
