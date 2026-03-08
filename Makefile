.PHONY: setup test lint build help

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

help:
	@echo "Available commands:"
	@echo "  make setup  - mvn clean install -DskipTests for chimera-core"
	@echo "  make test   - mvn test for chimera-core"
	@echo "  make lint   - mvn checkstyle:check for chimera-core"
	@echo "  make build  - mvn package -DskipTests for chimera-core"
	@echo "  make help   - show this help message"

