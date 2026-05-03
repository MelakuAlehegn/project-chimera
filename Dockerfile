# ----- Stage 1: build the fat JAR -----
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copy pom first so dependency download layer caches across source changes
COPY chimera/chimera-core/pom.xml ./pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline

# Copy sources and build the shaded JAR
COPY chimera/chimera-core/src ./src
COPY chimera/chimera-core/checkstyle.xml ./checkstyle.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B package -DskipTests

# ----- Stage 2: runtime -----
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /build/target/chimera-core-1.0-SNAPSHOT.jar /app/app.jar

# JVM tuning for container memory
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
