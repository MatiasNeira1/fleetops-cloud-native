# API REST local de FleetOps

Esta API expone las operaciones del panel operativo. El frontend consume estos endpoints mediante `VITE_API_BASE_URL` o mediante el proxy local de Vite.

## Flujo

```text
React -> Spring Boot REST -> PostgreSQL
                         -> RabbitMQ -> consumidor de telemetría -> PostgreSQL
```

Las consultas REST son síncronas. La telemetría responde `202 Accepted` después de publicarse; posición y alertas se actualizan de forma asíncrona. PostgreSQL y RabbitMQ no comparten una transacción atómica. Las operaciones que guardan y publican tienen riesgo de dual write; Outbox queda como mejora futura.

## Ejecución local

Desde PowerShell, crea `.env` a partir de `.env.example`, configura credenciales locales y levanta servicios sin borrar volúmenes:

```powershell
docker compose --env-file .env -f platform/local/docker-compose.yml up -d postgres rabbitmq-1 rabbitmq-2
./scripts/windows/local-backend-run.ps1
```

La API escucha en `http://localhost:8080`. El script carga las variables PostgreSQL y las credenciales RabbitMQ desde `.env` sin imprimirlas; en Windows define las direcciones de RabbitMQ como `localhost:5672,localhost:5673`.

Las credenciales `POSTGRES_USER`, `POSTGRES_PASSWORD`, `SPRING_RABBITMQ_USERNAME` y `SPRING_RABBITMQ_PASSWORD` son obligatorias; no tienen valores por defecto en Spring. En un contenedor conectado a la red Compose, configura `SPRING_RABBITMQ_ADDRESSES=rabbitmq-1:5672,rabbitmq-2:5672`.

Los datos demo se insertan únicamente con el perfil `local` o `dev` y `FLEETOPS_DEMO_DATA_ENABLED=true`. `.env.example` deja ese valor activo para poder probar endpoints REST de inmediato. Si la base ya tenía tablas pobladas, el inicializador no duplica datos; para volver a sembrar desde cero usa `./scripts/windows/local-clean.ps1` y levanta la infraestructura nuevamente.

## Smoke test

Con Docker arriba y el backend iniciado, ejecuta todos los endpoints principales con:

```powershell
./scripts/windows/api-smoke-test.ps1
```

En Bash:

```bash
./scripts/unix/api-smoke-test.sh
```

El smoke test espera `GET /actuator/health`, consulta viajes, detalle, alertas y posición, publica telemetría, crea un incidente y luego lo marca como `RESOLVED`.

## Frontend

El frontend lee `frontend/.env.local`:

```dotenv
VITE_API_BASE_URL=
```

En desarrollo local, deja la variable vacía para que el navegador llame rutas relativas como `/api/v1/trips`; `frontend/vite.config.ts` usa proxy hacia `http://localhost:8080`. Para apuntar directo a otro backend, configura:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

Reinicia `npm run dev` cada vez que cambies variables `VITE_*`.

## Endpoints

| Método y ruta | Éxito | Función |
| --- | --- | --- |
| `GET /api/v1/trips?status=IN_TRANSIT` | `200` | Lista viajes no completados ni cancelados; filtro opcional por estado. |
| `GET /api/v1/trips/{id}` | `200` | Detalle, conductor, vehículo, última posición, progreso y coordenadas de origen/destino. |
| `GET /api/v1/trips/{id}/alerts` | `200` | Alertas; filtros `status` y `severity`; devuelve arreglo vacío si no hay. |
| `GET /api/v1/vehicles/{id}/position` | `200` | Última posición. `404` diferencia vehículo inexistente y vehículo sin posición. |
| `POST /api/v1/telemetry` | `202` | Valida referencias y publica el evento; reconoce `eventId` ya procesado. |
| `POST /api/v1/incidents` | `201` | Persiste incidente, marca el viaje y publica eventos; retorna `Location`. |
| `PATCH /api/v1/incidents/{id}/status` | `200` | Cambia estado permitido y publica evento. |

Los errores usan `ProblemDetail` con estado, título, detalle, ruta y timestamp; no incluyen SQL ni stack trace.

### Ejemplo de viaje

```json
{
  "id": "trip-001",
  "origin": { "name": "Santiago", "latitude": -33.4489, "longitude": -70.6693 },
  "destination": { "name": "Valparaiso", "latitude": -33.0472, "longitude": -71.6127 },
  "driver": { "id": "driver-001", "name": "Carlos Soto" },
  "vehicle": { "id": "vehicle-001", "plate": "FH-82-19" },
  "status": "IN_TRANSIT",
  "departureTime": "2026-10-11T12:00:00Z",
  "estimatedArrival": "2026-10-11T14:00:00Z",
  "progress": 58,
  "totalDistanceKm": 120,
  "travelledDistanceKm": 69.6
}
```

### Ejemplo de telemetría

```json
{
  "eventId": "8ef852a3-11c8-4dd7-8ec7-f78bf0aa0012",
  "vehicleId": "vehicle-001",
  "tripId": "trip-001",
  "latitude": -33.2321,
  "longitude": -71.0201,
  "speedKph": 82.5,
  "fuelPercent": 44.2,
  "odometerKm": 128540.7,
  "recordedAt": "2026-10-11T13:15:30Z"
}
```

El consumidor registra posición y `eventId`; cambia un viaje `SCHEDULED` a `IN_TRANSIT` y crea alertas `SPEEDING`/`LOW_FUEL` según `FLEETOPS_TELEMETRY_MAX_SPEED_KPH` y `FLEETOPS_TELEMETRY_MIN_FUEL_PERCENT`. Una vez persistido el evento, redelivery duplicado no repite el procesamiento. Dos POST concurrentes o repetidos antes del consumo pueden publicar duplicados; el consumidor descarta el duplicado después de guardar el primer evento.

### Posición, cambio de estado y error

```json
{
  "vehicleId": "vehicle-001",
  "tripId": "trip-001",
  "latitude": -33.2321,
  "longitude": -71.0201,
  "speedKph": 82.5,
  "fuelPercent": 44.2,
  "odometerKm": 128540.7,
  "recordedAt": "2026-10-11T13:15:30Z"
}
```

```json
{ "status": "RESOLVED", "resolutionNote": "El vehículo puede continuar" }
```

```json
{
  "type": "https://fleetops.local/problems/400",
  "title": "Error de validacion",
  "status": 400,
  "detail": "latitude: debe ser menor o igual que 90.0",
  "instance": "/api/v1/telemetry",
  "timestamp": "2026-10-11T13:16:00Z",
  "path": "/api/v1/telemetry"
}
```

### Ejemplo de incidente

```json
{
  "tripId": "trip-001",
  "vehicleId": "vehicle-001",
  "type": "MECHANICAL_FAILURE",
  "title": "Problema de temperatura",
  "description": "El sensor reportó temperatura elevada del motor",
  "severity": "CRITICAL",
  "latitude": -33.2321,
  "longitude": -71.0201
}
```

Transiciones permitidas: `OPEN -> ACKNOWLEDGED`, `OPEN -> RESOLVED`, `ACKNOWLEDGED -> RESOLVED`. El registro y la publicación no usan Outbox, por lo que persiste riesgo de dual write.

## RabbitMQ

Se conservan `fleetops.logs.direct`, `fleetops.events.topic`, `fleetops.dlx.direct`, las tres colas académicas y sus DLQ. La cola adicional `fleetops.telemetry.queue` se enlaza al topic con `fleetops.telemetry.received`; su DLQ `fleetops.telemetry.dlq` se enlaza por `fleetops.telemetry.dead`. El consumidor hace ack manual después del procesamiento y rechaza sin requeue al fallar, enviando el mensaje a DLQ sin reintentos infinitos.

Las definiciones RabbitMQ permiten crear la topología antes de iniciar el backend; `RabbitMQConfig` la declara también al arrancar Spring. Ambas configuraciones contienen la misma cola, argumentos y bindings de telemetría.

## OpenAPI y seguridad

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

Springdoc expone OpenAPI para revisión e importación posterior en APIM. La API todavía no valida JWT y no simula autenticación. Azure, APIM, MSAL y Entra ID no se configuran aquí.

## Compatibilidad con React

Los IDs, conductor, patente, estado, progreso y coordenadas corresponden conceptualmente con `frontend/src/types/fleet.ts`. El backend expresa ubicaciones como objetos numéricos y enums en inglés; el frontend usa etiquetas españolas y coordenadas de ruta `[latitud, longitud]`. `frontend/src/services/tripService.ts` adapta el contrato REST al modelo visual del dashboard.

## Pruebas

Desde `backend/servicios/servicio-flota`:

```powershell
./mvnw.cmd test
./mvnw.cmd package
```

Las pruebas unitarias usan mocks y no requieren RabbitMQ externo; cualquier comprobación integrada se reporta por separado.
