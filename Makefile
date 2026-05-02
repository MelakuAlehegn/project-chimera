.PHONY: setup test lint build run run-loop spec-check docker-test help

MVN=mvn
CORE_POM=chimera/chimera-core/pom.xml

setup:
	$(MVN) -f $(CORE_POM) clean install -DskipTests

test:
	$(MVN) -f $(CORE_POM) test

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

docker-test:
	docker build -t chimera-test .
	docker run --rm chimera-test

help:
	@echo "Available commands:"
	@echo "  make setup       - mvn clean install -DskipTests for chimera-core"
	@echo "  make test        - mvn test for chimera-core"
	@echo "  make lint        - mvn checkstyle:check for chimera-core"
	@echo "  make build       - mvn package -DskipTests for chimera-core"
	@echo "  make run         - run one pipeline cycle (real APIs, real post)"
	@echo "  make run-loop    - run continuously every CHIMERA_LOOP_INTERVAL_MINUTES"
	@echo "  make spec-check  - verify code aligns with specs/"
	@echo "  make docker-test - build and run tests inside Docker"
	@echo "  make help        - show this help message"

