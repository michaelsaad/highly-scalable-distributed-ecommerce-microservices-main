# Distributed ecommerce microservices — common operator targets.
# Usage: make <target>    (Git Bash / WSL / GNU Make)

ifeq ($(OS),Windows_NT)
  MVNW := mvnw.cmd
else
  MVNW := ./mvnw
endif

COMPOSE := docker compose
SKIP_TESTS ?= -DskipTests

.DEFAULT_GOAL := help

.PHONY: help \
	infra-up infra-down infra-ps infra-logs \
	infra-data infra-messaging infra-observability \
	build test clean \
	run-product run-order run-inventory run-notification run-gateway \
	dev dev-up dev-down dev-logs \
	prod prod-up prod-down prod-logs \
	health

help:
	@echo ""
	@echo "  Infrastructure"
	@echo "  --------------"
	@echo "  make infra-up              Start databases, Kafka, Keycloak, Grafana stack"
	@echo "  make infra-down            Stop all infrastructure containers"
	@echo "  make infra-ps              Show running infra containers"
	@echo "  make infra-logs            Follow infra logs"
	@echo "  make infra-data            Start only MongoDB + MySQL"
	@echo "  make infra-messaging       Start only Kafka / Schema Registry / Kafka UI"
	@echo "  make infra-observability   Start only Loki, Prometheus, Tempo, Grafana, Keycloak"
	@echo ""
	@echo "  Build"
	@echo "  -----"
	@echo "  make build                 Maven install all services (skip tests)"
	@echo "  make test                  Run tests for all services"
	@echo "  make clean                 Maven clean all services"
	@echo ""
	@echo "  Run (local JVM, infra must already be up)"
	@echo "  -----------------------------------------"
	@echo "  make run-product           product-service   :8080"
	@echo "  make run-order             order-service     :8081"
	@echo "  make run-inventory         inventory-service :8082"
	@echo "  make run-notification      notification-service :9876"
	@echo "  make run-gateway           api-gateway       :9000"
	@echo ""
	@echo "  Docker (full stack)"
	@echo "  -------------------"
	@echo "  make dev                   Start everything in dev mode (ports exposed)"
	@echo "  make dev-down              Stop dev stack"
	@echo "  make dev-logs              Follow dev stack logs"
	@echo "  make prod                  Start everything in prod mode (restricted ports)"
	@echo "  make prod-down             Stop prod stack"
	@echo "  make prod-logs             Follow prod stack logs"
	@echo ""
	@echo "  Ops"
	@echo "  ---"
	@echo "  make health                Hit /actuator/health on every service"
	@echo ""

infra-up: infra-data infra-messaging infra-observability
	@echo "Infrastructure is up."

infra-data:
	$(COMPOSE) -f product-service/docker-compose.yml up -d
	$(COMPOSE) -f order-service/docker-compose.yml up -d order-db
	$(COMPOSE) -f inventory-service/docker-compose.yml up -d

infra-messaging:
	$(COMPOSE) -f order-service/docker-compose.yml up -d

infra-observability:
	$(COMPOSE) -f api-gateway/docker-compose.yml up -d

infra-down:
	-$(COMPOSE) -f api-gateway/docker-compose.yml down
	-$(COMPOSE) -f order-service/docker-compose.yml down
	-$(COMPOSE) -f inventory-service/docker-compose.yml down
	-$(COMPOSE) -f product-service/docker-compose.yml down

infra-ps:
	docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

infra-logs:
	docker ps --format "{{.Names}}" | xargs -r docker logs -f --tail=50

build:
	cd product-service && $(MVNW) clean install $(SKIP_TESTS)
	cd order-service && $(MVNW) clean install $(SKIP_TESTS)
	cd inventory-service && $(MVNW) clean install $(SKIP_TESTS)
	cd notification-service && $(MVNW) clean install $(SKIP_TESTS)
	cd api-gateway && $(MVNW) clean install $(SKIP_TESTS)

test:
	cd product-service && $(MVNW) test
	cd order-service && $(MVNW) test
	cd inventory-service && $(MVNW) test
	cd notification-service && $(MVNW) test
	cd api-gateway && $(MVNW) test

clean:
	cd product-service && $(MVNW) clean
	cd order-service && $(MVNW) clean
	cd inventory-service && $(MVNW) clean
	cd notification-service && $(MVNW) clean
	cd api-gateway && $(MVNW) clean

run-product:
	cd product-service && $(MVNW) spring-boot:run

run-order:
	cd order-service && $(MVNW) spring-boot:run

run-inventory:
	cd inventory-service && $(MVNW) spring-boot:run

run-notification:
	cd notification-service && $(MVNW) spring-boot:run

run-gateway:
	cd api-gateway && $(MVNW) spring-boot:run

# ── Docker full-stack targets ────────────────────────────────

dev: dev-up
dev-up:
	$(COMPOSE) --env-file .env.dev -f docker-compose.yml -f docker-compose.dev.yml up -d --build

dev-down:
	$(COMPOSE) --env-file .env.dev -f docker-compose.yml -f docker-compose.dev.yml down

dev-logs:
	$(COMPOSE) --env-file .env.dev -f docker-compose.yml -f docker-compose.dev.yml logs -f --tail=100

prod: prod-up
prod-up:
	$(COMPOSE) --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml up -d --build

prod-down:
	$(COMPOSE) --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml down

prod-logs:
	$(COMPOSE) --env-file .env.prod -f docker-compose.yml -f docker-compose.prod.yml logs -f --tail=100

health:
	@echo "product     :" && curl -sf http://localhost:8080/actuator/health || echo "down"
	@echo "order       :" && curl -sf http://localhost:8081/actuator/health || echo "down"
	@echo "inventory   :" && curl -sf http://localhost:8082/actuator/health || echo "down"
	@echo "notification:" && curl -sf http://localhost:9876/actuator/health || echo "down"
	@echo "gateway     :" && curl -sf http://localhost:9000/actuator/health || echo "down"
