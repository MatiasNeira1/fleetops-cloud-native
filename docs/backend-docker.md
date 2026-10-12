# Backend Docker local

Esta guia describe como construir y ejecutar `backend/servicios/servicio-flota` en Docker para desarrollo local. La imagen queda preparada para un despliegue posterior en Azure Container Apps, sin incluir secretos ni herramientas de compilacion en la capa final.

## Imagen

El Dockerfile usa Java 21 en build multi-stage:

- Builder: `eclipse-temurin:21-jdk-jammy`
- Runtime: `eclipse-temurin:21-jre-jammy`
- Usuario de runtime: `fleetops`, sin privilegios root
- Puerto expuesto: `8080`
- Ejecucion: `java -jar /app/app.jar`

La memoria se configura externamente con `JAVA_TOOL_OPTIONS`, por ejemplo:

```bash
JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
```

No guardes passwords, tokens, certificados ni cadenas de conexion reales en el Dockerfile, en la imagen ni en Git.

## Construir

Desde la raiz del repositorio:

```bash
docker build -t fleetops/servicio-flota:local backend/servicios/servicio-flota
```

Tambien puedes construirlo mediante Compose:

```bash
docker compose --env-file .env -f platform/local/docker-compose.yml build servicio-flota
```

## Variables necesarias

El backend mantiene la misma configuracion de Spring Boot:

| Variable | Uso |
| --- | --- |
| `SERVER_PORT` | Puerto HTTP. Predeterminado: `8080`. |
| `POSTGRES_HOST` | Host PostgreSQL. En Docker: `postgres`. |
| `POSTGRES_PORT` | Puerto PostgreSQL. En Docker: `5432`. |
| `POSTGRES_DB` | Base de datos. Predeterminado local: `fleetops`. |
| `POSTGRES_USER` | Usuario PostgreSQL. Requerido en `.env`. |
| `POSTGRES_PASSWORD` | Password PostgreSQL. Requerido en `.env`. |
| `SPRING_RABBITMQ_ADDRESSES` | Lista de nodos RabbitMQ. |
| `SPRING_RABBITMQ_USERNAME` | Usuario RabbitMQ. Requerido en `.env`. |
| `SPRING_RABBITMQ_PASSWORD` | Password RabbitMQ. Requerido en `.env`. |
| `FLEETOPS_DEMO_DATA_ENABLED` | Carga datos ficticios locales cuando corresponde. |
| `FLEETOPS_TELEMETRY_MAX_SPEED_KPH` | Umbral de velocidad. |
| `FLEETOPS_TELEMETRY_MIN_FUEL_PERCENT` | Umbral de combustible. |

## Direcciones locales vs Docker

Cuando el backend corre directamente en Windows, Maven o IntelliJ, usa los puertos publicados:

```dotenv
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
SPRING_RABBITMQ_ADDRESSES=localhost:5672,localhost:5673
```

Cuando el backend corre dentro de Docker Compose, usa los nombres DNS internos de la red `fleetops-local`:

```dotenv
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
SPRING_RABBITMQ_ADDRESSES=rabbitmq-1:5672,rabbitmq-2:5672
```

No uses `localhost` desde un contenedor para conectarte a PostgreSQL o RabbitMQ; dentro del contenedor `localhost` es el propio contenedor.

## Levantar el stack

Desde la raiz del repositorio:

```bash
docker compose --env-file .env -f platform/local/docker-compose.yml up -d postgres rabbitmq-1 rabbitmq-2 servicio-flota
```

El compose conserva PostgreSQL y el cluster RabbitMQ de dos nodos. `servicio-flota` espera a que esos servicios esten saludables cuando Docker Compose puede evaluarlo.

## Verificar

Health check HTTP:

```bash
curl http://localhost:8080/actuator/health
```

Endpoint de viajes:

```bash
curl http://localhost:8080/api/v1/trips
```

Logs:

```bash
docker compose --env-file .env -f platform/local/docker-compose.yml logs servicio-flota
```

El endpoint Actuator expuesto por web es `health`. No se exponen endpoints Actuator con informacion sensible.

## Azure Container Apps

Para Azure Container Apps se debe publicar la imagen en ACR con una etiqueta inmutable, no `latest`. Las variables anteriores deben configurarse como environment variables o secrets del entorno de Container Apps. PostgreSQL debera apuntar al host de Azure Database for PostgreSQL o al servicio elegido; RabbitMQ debera apuntar a los hosts reales disponibles para ese entorno.

Ejemplo de etiquetado futuro:

```bash
docker tag fleetops/servicio-flota:local <acr-name>.azurecr.io/fleetops/servicio-flota:<version>
docker push <acr-name>.azurecr.io/fleetops/servicio-flota:<version>
```
