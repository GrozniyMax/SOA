# SOA Lab — OpenAPI спецификации

OpenAPI 3.0 спецификации для двух веб-сервисов и локальный Swagger UI для их просмотра.

## Структура

```
openapi/
  movie-service.yaml   # Спецификация 1-го сервиса (управление коллекцией Movie)
  oscar-service.yaml   # Спецификация 2-го сервиса (базовый путь /oscar)
docs/
  index.html           # Swagger UI с переключателем между спеками
```

## Запуск локального Swagger UI

Из корня проекта:

```bash
python3 -m http.server 8080
```

Затем откройте в браузере: <http://localhost:8080/docs/>

В верхней панели можно переключаться между **Movie Collection Service** и **Oscar Service**.

### Альтернатива через Docker

Если интернет недоступен или нужен отдельный контейнер:

```bash
docker run --rm -p 8080:8080 \
  -v "$PWD/openapi:/specs" \
  -e SWAGGER_JSON=/specs/movie-service.yaml \
  swaggerapi/swagger-ui
```

## Содержимое спецификаций

### Movie Collection Service (`openapi/movie-service.yaml`)

Управление коллекцией объектов `Movie`. URL в kebab-case.

| Метод | Путь | Описание | Коды ответов |
|-------|------|----------|--------------|
| POST | `/movies` | Добавить элемент | 201, 400, 406, 415, 422, 500 |
| GET | `/movies` | Массив с сортировкой/фильтрацией/пагинацией | 200, 400, 406, 500 |
| POST | `/movies/search` | Массив с фильтром в теле запроса | 200, 400, 406, 415, 500 |
| GET | `/movies/{id}` | Получить по ИД | 200, 400, 404, 406, 500 |
| PUT | `/movies/{id}` | Обновить по ИД | 200, 400, 404, 406, 415, 422, 500 |
| DELETE | `/movies/{id}` | Удалить по ИД | 204, 400, 404, 500 |
| GET | `/movies/max-tagline` | Объект с максимальным tagline | 200, 404, 406, 500 |
| GET | `/movies/directors-greater-than` | Объекты, где director больше заданного | 200, 400, 406, 500 |
| GET | `/movies/genres` | Уникальные genre | 200, 406, 500 |

### Oscar Service (`openapi/oscar-service.yaml`)

Дополнительные операции, базовый путь `/oscar`.

| Метод | Путь | Описание | Коды ответов |
|-------|------|----------|--------------|
| GET | `/oscar/operators/losers` | Операторы без Оскаров | 200, 406, 502, 503, 504, 500 |
| POST | `/oscar/directors/humiliate-by-genre/{genre}` | Отобрать Оскары по жанру | 200, 400, 406, 502, 503, 504, 500 |

### Семантика кодов ответов

| Код | Значение |
|-----|----------|
| 200 OK | Успешный запрос с телом ответа |
| 201 Created | Объект создан (POST) |
| 204 No Content | Успешный запрос без тела (DELETE) |
| 400 Bad Request | Некорректный формат данных / невалидные параметры |
| 404 Not Found | Объект не найден |
| 406 Not Acceptable | Заголовок Accept не допускает application/json |
| 415 Unsupported Media Type | Тело запроса не в формате application/json |
| 422 Unprocessable Entity | Нарушение ограничений целостности |
| 500 Internal Server Error | Внутренняя ошибка сервера |
| 502 Bad Gateway | Первый сервис вернул некорректный ответ |
| 503 Service Unavailable | Первый сервис временно недоступен |
| 504 Gateway Timeout | Первый сервис не ответил вовремя |

## Валидация спецификаций

```bash
python3 -c "import yaml,sys; yaml.safe_load(open('openapi/movie-service.yaml'))"
python3 -c "import yaml,sys; yaml.safe_load(open('openapi/oscar-service.yaml'))"
```

Опционально (требует Node.js):

```bash
npx @redocly/cli lint openapi/movie-service.yaml
npx @redocly/cli lint openapi/oscar-service.yaml
```