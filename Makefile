.PHONY: help local-up local-down local-restart local-logs local-ps local-clean env-check git-status

COMPOSE_FILE := platform/local/docker-compose.yml
ENV_FILE_OPTION := $(if $(wildcard .env),--env-file .env,)
DOCKER_COMPOSE := docker compose $(ENV_FILE_OPTION) -f $(COMPOSE_FILE)

help:
	@echo "FleetOps - comandos disponibles"
	@echo ""
	@echo "  make local-up       Levanta la infraestructura local"
	@echo "  make local-down     Detiene la infraestructura local"
	@echo "  make local-restart  Reinicia la infraestructura local"
	@echo "  make local-logs     Muestra logs de Docker Compose"
	@echo "  make local-ps       Lista contenedores locales"
	@echo "  make local-clean    Detiene el entorno y elimina volumenes locales"
	@echo "  make env-check      Verifica archivos de entorno esperados"
	@echo "  make git-status     Muestra el estado Git"

local-up:
	$(DOCKER_COMPOSE) up -d

local-down:
	$(DOCKER_COMPOSE) down

local-restart: local-down local-up

local-logs:
	$(DOCKER_COMPOSE) logs -f

local-ps:
	$(DOCKER_COMPOSE) ps

local-clean:
	$(DOCKER_COMPOSE) down -v --remove-orphans

env-check:
	@test -f .env.example || (echo "ERROR: falta .env.example" && exit 1)
	@if [ -f .env ]; then echo "OK: .env local encontrado"; else echo "WARN: .env no existe. Copia .env.example a .env para personalizar valores locales."; fi

git-status:
	git status
