# Лаба №3 (hoxton): сборка и управление docker-compose-стеком.

COMPOSE = docker compose -f deploy/docker-compose.yml --env-file deploy/.env
CERTS_DIR = deploy/certs
GRADLE = ./gradlew
# Фронт ходит на API same-origin через nginx-проксирование — пути относительные.
VITE_ENV = VITE_MOVIE_API=/movie-service VITE_OSCAR_API=/oscar

.PHONY: all build certs env up down restart logs ps test test-infra clean purge

all: build up

## build: jar-ы всех пяти модулей (appDist привязан к assemble) + фронтенд (Vite)
build:
	$(GRADLE) :movie-service:assemble :oscar-service:assemble :config-server:assemble :eureka-server:assemble :api-gateway:assemble
	$(VITE_ENV) npm --prefix frontend run build

## certs: генерация self-signed сертификатов для gateway и nginx
certs:
	bash deploy/gen-certs.sh

## env: создать deploy/.env из шаблона, если ещё нет (идемпотентно)
env:
	@test -f deploy/.env || cp deploy/.env.example deploy/.env

## up: поднять стек (env + сертификаты обязательны, jar-ы должны быть собраны)
up: env
	@if [ ! -f $(CERTS_DIR)/gateway.p12 ]; then \
		echo "Нет $(CERTS_DIR)/gateway.p12 — сначала выполните: make certs"; \
		exit 1; \
	fi
	$(COMPOSE) up -d --build

down:
	$(COMPOSE) down

restart:
	$(COMPOSE) restart

logs:
	$(COMPOSE) logs -f --tail=100

ps:
	$(COMPOSE) ps

test:
	$(GRADLE) test

## test-infra: инфраструктурные pytest-тесты (проверяют поднятый стек)
test-infra:
	python3 -m pytest tests/infra/ -v

## clean: без удаления volume БД; для полного сброса есть purge
clean:
	$(GRADLE) clean
	$(COMPOSE) down

## purge: ДЕСТРУКТИВНО — останавливает стек, удаляет volume БД и сертификаты
purge:
	$(COMPOSE) down -v
	rm -rf $(CERTS_DIR)
