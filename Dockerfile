FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper and pom first for layer caching
COPY chimera/chimera-core/pom.xml chimera/chimera-core/pom.xml

# Resolve dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f chimera/chimera-core/pom.xml dependency:resolve dependency:resolve-plugins -q

# Copy sources
COPY chimera/chimera-core/src chimera/chimera-core/src
COPY chimera/chimera-core/checkstyle.xml chimera/chimera-core/checkstyle.xml

# Copy Makefile
COPY Makefile .

# Default: run the tests
CMD ["make", "test"]
