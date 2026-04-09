.PHONY: setup test lint build spec-check docker-test help

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
	@echo "  make spec-check  - verify code aligns with specs/"
	@echo "  make docker-test - build and run tests inside Docker"
	@echo "  make help        - show this help message"

