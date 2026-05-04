.PHONY: setup test test-unit lint build run run-loop spec-check docker-up docker-down docker-logs docker-test help

MVN=mvn
CORE_POM=chimera/chimera-core/pom.xml

setup:
	$(MVN) -f $(CORE_POM) clean install -DskipTests

test:
	$(MVN) -f $(CORE_POM) test

# CI-friendly: skips tests tagged @Tag("integration") (Postgres, Gemini, Bluesky, MCP).
test-unit:
	$(MVN) -f $(CORE_POM) test -DexcludedGroups=integration

lint:
	$(MVN) -f $(CORE_POM) checkstyle:check

build:
	$(MVN) -f $(CORE_POM) package -DskipTests

run:
	@CHIMERA_RUN_MODE=once $(MVN) -f $(CORE_POM) -q exec:java \
		-Dexec.mainClass=com.chimera.App

run-loop:
	@CHIMERA_RUN_MODE=loop $(MVN) -f $(CORE_POM) -q exec:java \
		-Dexec.mainClass=com.chimera.App

spec-check:
	@bash scripts/spec-check.sh

docker-up:
	docker compose up --build

docker-down:
	docker compose down

docker-logs:
	docker compose logs -f app

help:
	@echo "Available commands:"
	@echo "  make setup       - mvn clean install -DskipTests for chimera-core"
	@echo "  make test        - run all tests (incl. integration; needs .env, Postgres, Node)"
	@echo "  make test-unit   - run unit tests only (CI uses this)"
	@echo "  make lint        - mvn checkstyle:check for chimera-core"
	@echo "  make build       - mvn package -DskipTests for chimera-core"
	@echo "  make run         - run one cycle locally (real APIs, real post)"
	@echo "  make run-loop    - run continuously locally"
	@echo "  make spec-check  - verify code aligns with specs/"
	@echo "  make docker-up   - build images and start stack (db + app)"
	@echo "  make docker-down - stop and remove the stack"
	@echo "  make docker-logs - follow the app container logs"
	@echo "  make help        - show this help message"

