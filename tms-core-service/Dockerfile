# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    apt-get update && apt-get install -y --no-install-recommends maven && \
    mvn -B -q -DskipTests -Dspotless.check.skip=true package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 tms
WORKDIR /app
COPY --from=build /workspace/target/tms-core-service-*.jar /app/tms-core-service.jar
USER 10001
EXPOSE 8080
# All configuration is supplied through TMS_* environment variables (see .env.example).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/tms-core-service.jar"]
