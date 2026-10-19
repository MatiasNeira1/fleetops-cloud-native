FROM eclipse-temurin:21-jdk-jammy AS dependencies

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

FROM dependencies AS builder

COPY src src
RUN ./mvnw -B -ntp clean package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd --system fleetops \
    && useradd --system --gid fleetops --home-dir /app --shell /usr/sbin/nologin fleetops

COPY --chown=fleetops:fleetops --from=builder /workspace/target/servicio-flota-*.jar /app/app.jar

USER fleetops

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
