FROM maven:3.9.11-eclipse-temurin-21@sha256:6fdc855a6ed81d288ca7ca37ac6ff5e9308b612485c0801d70b25a858c83d237 AS builder
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src src
RUN mvn -B -ntp package

FROM eclipse-temurin:21.0.10_7-jre-jammy@sha256:fa4854e6057665066cb79953616671c20f32b96a85d6dd8b78db0762203924c4
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --system fleetops && useradd --system --gid fleetops --home-dir /app --shell /usr/sbin/nologin fleetops
COPY --chown=fleetops:fleetops --from=builder /workspace/target/ms-alerta-*.jar /app/app.jar
USER fleetops
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD curl -fsS http://127.0.0.1:8081/actuator/health/readiness >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
