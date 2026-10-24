# Лаба №3 — сравнение вариантов реализации: план A vs план B

Оба варианта полностью реализуют задание лабы №3 (Config Service, Eureka, Ribbon, Zuul Proxy,
второй экземпляр oscar + HAProxy, сохранение API лабы №2) и прошли одинаковую верификацию:
61 юнит-тест сервисов зелёный, 13/13 инфраструктурных pytest-тестов (включая холодный старт стека
и haproxy-failover), полный docker-стек с TLS на gateway :8443 и фронтенде :8444.

| | **План A — ветка `lab3`** («legacy-остров») | **План B — ветка `lab3-hoxton`** («всё на Hoxton») |
|---|---|---|
| **Идея** | Сервисы остаются на современном стеке; Zuul+Ribbon изолированы в отдельном legacy-модуле api-gateway | Весь проект переводится на линейку, где Zuul+Ribbon «родные» |
| **movie/oscar** | Spring Boot 3.5.3, Java 21, jakarta, Hibernate 6, OpenFeign 4.3 (2025.0.0) | Spring Boot 2.3.12, Java 11, javax, Hibernate 5, OpenFeign 10.12 (Hoxton.SR12) |
| **api-gateway** | Boot 2.3 + Hoxton + Java 11 — единственный legacy-модуль; исключён из корневой конфигурации | Тот же Hoxton, что и всё остальное — «острова» нет |
| **config-server / eureka-server** | Boot 3.5 (spring-cloud 2025.0.0) | Boot 2.3 (Hoxton) |
| **Объём изменений кода сервисов** | Минимальный: WAR→JAR, конфиги, X-Instance; домен/бизнес-логика не тронуты | Полная миграция: 14 рекордов → классы, jakarta→javax, Criteria/HQL под Hibernate 5, Feign-клиент переписан вручную (47 файлов, +508/−342) |
| **Feign-клиент oscar** | Генерируется из OpenAPI (генератор, jakarta) | Пишется вручную: OpenFeign 10.12 поддерживает только одноуровневое наследование интерфейсов — клиент переобъявляет `searchMovies`/`humiliateDirectors` с `@RequestLine` |
| **Конфиг-клиент** | Boot 3: `spring.config.import: optional:configserver:`; remote-источник ниже локального yml | Hoxton: `bootstrap.yml`; `override-none` задаётся **со стороны сервера** (клиентские флаги bind'ятся из remote-источников), а `server.port` параметризуется локально — иначе env не пробрасывается |
| **Упаковка** | movie/oscar/config/eureka — `bootJar` (Boot-плагин); gateway — classpath-дистрибутив `gatewayDist` | `appDist` classpath-дистрибутив у **всех** приложений (Boot-плагин 2.x несовместим с Gradle 8) |
| **Топология docker** | Идентична: postgres → config+eureka → movie + oscar-1/2 → haproxy → gateway (TLS 8443) → nginx-фронт (TLS 8444) | Та же; compose-проект `soa-hoxton` |
| **Грабли, решённые на практике** | shadow-jar ломает `spring.factories` (дубли ключей → Zuul-автоконфигурация теряется); compose merge key не мерджит env-карты; haproxy docker-DNS resolvers; bash без `/dev/tcp` → curl healthchecks | Всё из A плюс: вложенные BOM Hoxton не разворачиваются dependency-management-плагином (явные импорты BOM + feign-bom 10.12); bulk-update HQL подзапросом; `@MockBean` вместо `@MockitoBean`; `-parameters` для MapStruct; Ribbon-кэш ~30с после пересоздания инстансов |
| **EOL-риск** | Локализован в одном новом модуле (api-gateway) | На весь проект: патчи безопасности Hoxton/Boot 2.3 прекращены ~2022 |
| **Соответствие букве задания** | Zuul/Ribbon «настоящие» (Hoxton-артефакты), но формулировка «переработать оба сервиса» читается с оговоркой: сервисы остались на Boot 3.5 | Максимально дословное: вся платформа — Spring Cloud, Zuul/Ribbon нативны для всего проекта |
| **Актуальность кода** | Домен на Java 21 (records, современный JPA) — легко развивать дальше | Домен откачен на Java 11/javax — развитие ограничено EOL-линейкой |
| **Трудозатраты** | ~18 коммитов, из них бо́льшая часть — инфраструктура/deploy; код сервисов почти не тронут | ~14 коммитов, из них половина — миграция кода; каждое будущее изменение пишется на javax/Hibernate 5 |

## Как это выглядит снаружи

Для пользователя оба варианта неотличимы: одни и те же URL (`https://host:8444` фронт,
`https://host:8443/movie-service|/oscar` API через gateway), один и тот же контракт лабы №2,
одни и те же Makefile-таргеты (`build`, `certs`, `up`, `test`, `test-infra`, `down`, `purge`)
и одинаковый набор инфраструктурных тестов.

## Что выбрать

- **Для сдачи лабы «дословно по тексту задания»** — план B (`lab3-hoxton`): вся платформа
  Spring Cloud, Zuul и Ribbon демонстрируются нативно, вопросов «а почему сервисы остались
  на Boot 3» не возникает.
- **Если важна живучесть кода после лабы** — план A (`lab3`): бизнес-код остаётся на
  современном стеке и продолжает развиваться, legacy-риск заперт в одном модуле.
- **Обучающая ценность** у плана B выше: миграция вскрывает реальные отличия
  jakarta↔javax, Hibernate 5↔6, Feign 10↔13 и механику bootstrap-конфигурации Spring Cloud
  «как было» до Boot 3.

## Ветки

| Ветка | Вариант | Состояние |
|---|---|---|
| `lab3` (= `lab3-normal`) | План A | Готова, ревью «ready to merge», docs: [architecture-lab3.md](architecture-lab3.md) |
| `lab3-hoxton` | План B | Готова, ревью «Ready», docs: `docs/architecture-hoxton.md` в этой ветке |

Документация по внутренностям каждого варианта:
- План A: [architecture-lab3.md](architecture-lab3.md) + интерактивная схема [architecture-lab3.html](architecture-lab3.html)
- План B: `docs/architecture-hoxton.md` в ветке `lab3-hoxton` (маршруты, грабли Hoxton, сборка)
