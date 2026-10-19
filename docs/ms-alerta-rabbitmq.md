# Alertas asíncronas con ms-alerta

## Arquitectura y responsabilidades

`servicio-flota` conserva `POST /api/v1/telemetry`: valida vehículo y viaje, responde HTTP 202 y publica el evento de telemetría que ya existía. Su consumidor de telemetría persiste posición, evalúa las reglas existentes (velocidad > `FLEETOPS_TELEMETRY_MAX_SPEED_KPH`, combustible < `FLEETOPS_TELEMETRY_MIN_FUEL_PERCENT`) y crea alertas en sus tablas actuales. Por cada alerta creada, prepara un evento JSON y lo publica **después del commit** local. No publica al hacer rollback ni por telemetría normal o duplicada. `ms-alerta` consume el evento, guarda su propia copia en `alerts.alerts` y ofrece una API REST. El navegador solo usa HTTP; RabbitMQ es interno.

```text
POST /api/v1/telemetry -> servicio-flota -> fleetops.telemetry.queue
-> reglas y commit -> fleetops.events.topic
-> fleetops.alerts.queue -> ms-alerta -> alerts.alerts
-> GET /api/v1/trips/{tripId}/alerts
```

La respuesta 202 indica aceptación, no persistencia inmediata en `ms-alerta`; la consulta puede requerir unos segundos. Los endpoints existentes de `servicio-flota` para consultar alertas por viaje y resolverlas permanecen activos. Una futura regla de APIM puede dirigir las rutas de alertas a `ms-alerta`; antes de hacerlo se debe adaptar el cliente a la respuesta paginada y a los estados nuevos.

## Contrato JSON v1

Ejemplo de evento de velocidad (identificadores ficticios):

```json
{
  "eventId": "11111111-1111-4111-8111-111111111111",
  "eventType": "alert.created",
  "eventVersion": 1,
  "occurredAt": "2026-10-17T12:00:00Z",
  "source": "servicio-flota",
  "correlationId": "22222222-2222-4222-8222-222222222222",
  "alertId": 42,
  "tripId": "trip-001",
  "vehicleId": "vehicle-001",
  "alertType": "SPEED_LIMIT_EXCEEDED",
  "severity": "WARNING",
  "title": "Exceso de velocidad",
  "message": "Velocidad reportada sobre el limite configurado",
  "measuredValue": 105,
  "thresholdValue": 90,
  "metadata": {}
}
```

`eventId` es UUID único de publicación; `correlationId` es el `eventId` enviado al endpoint HTTP de telemetría, propagado por el evento intermedio, y permite seguir el flujo. `occurredAt` usa UTC. El tipo heredado `SPEEDING` se publica como `SPEED_LIMIT_EXCEEDED`; `LOW_FUEL` conserva su nombre. No se incluyen JWT, cabeceras Authorization ni credenciales. Los mensajes son persistentes, con `application/json` y `JacksonJsonMessageConverter`.

## Topología y entregas

| Elemento | Nombre o valor |
| --- | --- |
| Exchange topic existente | `fleetops.events.topic` (durable) |
| Routing velocidad | `fleetops.alert.speed.created` |
| Routing combustible | `fleetops.alert.fuel.created` |
| Cola principal nueva | `fleetops.alerts.queue` (durable) |
| Binding principal | `fleetops.alert.#` |
| Dead-letter exchange existente | `fleetops.dlx.direct` (durable) |
| DLQ nueva | `fleetops.alerts.dlq` (durable) |
| Dead-letter routing key y binding | `fleetops.alerts.dlq` |

La cola principal declara `x-dead-letter-exchange=fleetops.dlx.direct` y `x-dead-letter-routing-key=fleetops.alerts.dlq`. `platform/local/rabbitmq/definitions.json` declara la topología inicial antes de que arranquen los servicios; `ms-alerta` vuelve a declarar exchange, colas y bindings de forma idempotente, también cuando ya existen volúmenes RabbitMQ. Se conservan `fleetops.logs.direct`, las tres colas de logs y sus DLQ. La unión amplia `fleetops.#` de la cola general de logs ya existente también observa eventos de alerta, pero no es usada como cola de negocio.

El consumidor usa ack manual después de que la transacción de persistencia termina. Rechaza JSON inválido y eventos no procesables con `basicNack(tag, false, false)`. Reintenta fallos transitorios hasta tres veces en la misma entrega, sin requeue infinito; luego rechaza y RabbitMQ mueve a DLQ. `prefetch=10` limita mensajes sin confirmar **por consumidor**. `source_event_id` tiene restricción `UNIQUE`; la consulta previa evita duplicados habituales y la restricción resuelve carreras. Los logs registran `eventId`, `correlationId` y resultado, sin payload completo.

`servicio-flota` usa `RabbitTemplate`, publisher confirms correlacionados y returns, con `mandatory=true`. No espera confirmación en la petición HTTP. Existe una ventana residual entre el commit de PostgreSQL y la publicación: si el proceso cae allí, la alerta local existe pero el mensaje puede perderse. También puede perderse ante NACK/return después del commit; revisar logs y reconciliar manualmente. Un outbox transaccional es el siguiente paso si se requiere entrega garantizada. Una confirmación positiva del broker tampoco garantiza que el consumidor haya persistido; la idempotencia protege reentregas.

## Ejecutar localmente

1. Copiar `.env.example` a `.env` y sustituir contraseñas de ejemplo. No versionar `.env`. Mantener `FLEETOPS_DEMO_DATA_ENABLED=true` para los IDs de demostración.
2. En Windows: `./scripts/windows/local-up.ps1`. En Unix: `./scripts/unix/local-up.sh`. Compose construye `ms-alerta` y lo conecta a `postgres:5432` y `rabbitmq-1:5672,rabbitmq-2:5672`. Desde Windows fuera de Docker, usar `SPRING_RABBITMQ_ADDRESSES=localhost:5672,localhost:5673`.
3. Comprobar `http://127.0.0.1:8080/actuator/health`, `http://127.0.0.1:8081/actuator/health`, `http://127.0.0.1:8081/actuator/health/liveness` y `/actuator/health/readiness`. La readiness de `ms-alerta` incluye PostgreSQL y RabbitMQ; Actuator no muestra detalles sensibles.
4. Ejecutar `./scripts/windows/ms-alerta-demo.ps1` o `bash scripts/unix/ms-alerta-demo.sh` (requiere `curl` y `jq`). Esperan servicios saludables, comparan alertas y ACK antes/después, envían velocidad y combustible, verifican el `correlationId` único de cada envío y comparan la DLQ antes/después de un mensaje inválido. No purgan colas ni usan mensajes antiguos como prueba.

El DTO real de telemetría es `eventId`, `vehicleId`, `tripId`, `latitude`, `longitude`, `speedKph`, `fuelPercent`, `odometerKm`, `recordedAt`. Para Postman, crear `POST http://127.0.0.1:8080/api/v1/telemetry` con `Content-Type: application/json` y cuerpo:

```json
{"eventId":"22222222-2222-4222-8222-222222222222","vehicleId":"vehicle-001","tripId":"trip-001","latitude":-33.2321,"longitude":-71.0201,"speedKph":105,"fuelPercent":50,"odometerKm":128540.7,"recordedAt":"2026-10-17T12:00:00Z"}
```

Repetir con un `eventId` nuevo, `speedKph:50` y `fuelPercent:8` para combustible bajo. Equivalente curl: `curl -H 'Content-Type: application/json' -d '{"eventId":"22222222-2222-4222-8222-222222222222","vehicleId":"vehicle-001","tripId":"trip-001","latitude":-33.2321,"longitude":-71.0201,"speedKph":105,"fuelPercent":50,"odometerKm":128540.7,"recordedAt":"2026-10-17T12:00:00Z"}' http://127.0.0.1:8080/api/v1/telemetry`. Consultar `curl 'http://127.0.0.1:8081/api/v1/trips/trip-001/alerts?size=20'`.

RabbitMQ Management: `http://127.0.0.1:15672`, pestaña Queues. Revisar `fleetops.alerts.queue` (ACK creciente) y `fleetops.alerts.dlq` (mensajes tras rechazo controlado). El script publica el mensaje inválido mediante Management API sin consumir mensajes anteriores.

Para verificar el clúster desde la terminal: `docker exec fleetops-rabbitmq-1 rabbitmqctl cluster_status`. Para revisar errores después de la demostración: `docker logs --tail 100 fleetops-servicio-flota` y `docker logs --tail 100 fleetops-ms-alerta`. El script de demostración comprueba también que Management API informa dos nodos.

## API, datos y configuración

- `GET /api/v1/alerts?tripId=&vehicleId=&type=&severity=&status=&page=0&size=20`: filtros opcionales, respuesta `Page` de Spring.
- `GET /api/v1/alerts/{id}`: una alerta o 404 ProblemDetail.
- `GET /api/v1/trips/{tripId}/alerts?page=0&size=20`: alertas de un viaje, paginadas.
- `PATCH /api/v1/alerts/{id}/status` con `{"status":"ACKNOWLEDGED"}` o `{"status":"RESOLVED"}`: transiciones `ACTIVE -> ACKNOWLEDGED -> RESOLVED` o `ACTIVE -> RESOLVED`; transiciones inválidas devuelven 409.

Flyway crea el esquema `alerts` y la tabla `alerts.alerts` con `source_event_id UNIQUE` e índices por `trip_id`, `vehicle_id`, `status` y `occurred_at`. Hibernate usa `ddl-auto=validate`; `ms-alerta` no consulta las tablas internas de `servicio-flota`. Campos: ID, evento y alerta de origen, correlación, viaje, vehículo, tipo, severidad, título, mensaje, medición, umbral, estado y timestamps de ocurrencia, creación y actualización.

Variables: `SERVER_PORT` (8081), `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `SPRING_RABBITMQ_ADDRESSES`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`, `RABBITMQ_ALERTS_EXCHANGE`, `RABBITMQ_ALERTS_DLX`, `RABBITMQ_ALERTS_QUEUE`, `RABBITMQ_ALERTS_DLQ`, `RABBITMQ_ALERTS_ROUTING_PATTERN`, `RABBITMQ_ALERTS_DLQ_ROUTING_KEY`. Las credenciales se suministran desde `.env` o el entorno; no están en `application.properties` ni en Dockerfile.

## Límites y próximos pasos

No se introduce outbox, autenticación propia de la API ni migración automática de alertas históricas. La API nueva es eventualmente consistente y usa estados `ACTIVE/ACKNOWLEDGED/RESOLVED`, mientras que `servicio-flota` conserva `OPEN/RESOLVED`; el cliente y APIM necesitan una decisión de compatibilidad antes de cambiar rutas. Validar los despliegues reales con PostgreSQL, RabbitMQ y las reglas de red, publicar la imagen en ACR, configurar Container Apps con secretos/health probes y luego enrutar y proteger los endpoints de alertas en APIM. Nada de eso se aplica aquí.
