# RabbitMQ local

Esta guia describe el cluster RabbitMQ local usado por FleetOps para la demostracion academica de mensajeria.

## Arquitectura

Docker Compose levanta dos nodos RabbitMQ `4.1.8-management` en la misma red `fleetops-local`.

| Servicio | Hostname interno | Nodename | AMQP desde Windows | UI de administracion |
| --- | --- | --- | --- | --- |
| `rabbitmq-1` | `rabbitmq-1` | `rabbit@rabbitmq-1` | `localhost:5672` | `http://localhost:15672` |
| `rabbitmq-2` | `rabbitmq-2` | `rabbit@rabbitmq-2` | `localhost:5673` | `http://localhost:15673` |

Dentro de Docker ambos nodos escuchan AMQP en `5672`. Cada nodo usa un volumen persistente independiente:

- `fleetops-rabbitmq-1-data`
- `fleetops-rabbitmq-2-data`

Los nodos comparten `RABBITMQ_ERLANG_COOKIE`, tienen `RABBITMQ_NODENAME` distinto y usan descubrimiento estatico en `platform/local/rabbitmq/rabbitmq.conf`.

> Advertencia: este cluster de dos nodos se utiliza para cumplir la demostracion academica. No debe presentarse como alta disponibilidad de produccion. En RabbitMQ, una arquitectura productiva necesita un diseno de tolerancia a fallos mas cuidadoso; si se usan quorum queues, dos nodos no entregan tolerancia real a la perdida de un nodo porque se necesita mayoria.

## Variables

Copia `.env.example` a `.env` y define valores locales:

```bash
cp .env.production.local.example .env.production.local
```

RabbitMQ requiere estas variables:

```dotenv
RABBITMQ_DEFAULT_USER=fleetops
RABBITMQ_DEFAULT_PASS=change_me
RABBITMQ_ERLANG_COOKIE=replace_with_a_long_local_demo_cookie
```

`docker-compose.yml` valida esas variables con `${VARIABLE:?mensaje}`. No guardes secretos reales en Git. `.env.example` contiene solo valores ficticios.

Para ejecutar el backend directamente desde Windows, Maven o IntelliJ, usa los puertos publicados en el host:

```dotenv
SPRING_RABBITMQ_ADDRESSES=localhost:5672,localhost:5673
SPRING_RABBITMQ_USERNAME=fleetops
SPRING_RABBITMQ_PASSWORD=change_me
```

Para ejecutar el backend dentro de Docker o desde otro contenedor en la misma red, usa los hostnames internos:

```dotenv
SPRING_RABBITMQ_ADDRESSES=rabbitmq-1:5672,rabbitmq-2:5672
SPRING_RABBITMQ_USERNAME=fleetops
SPRING_RABBITMQ_PASSWORD=change_me
```

No uses `rabbitmq-1` ni `rabbitmq-2` desde un proceso que corre directamente en Windows: esos nombres solo resuelven dentro de la red Docker.

## Topologia

La topologia se conserva con los nombres que ya usa Spring Boot. RabbitMQ la carga automaticamente desde `platform/local/rabbitmq/definitions.json`, y Spring AMQP vuelve a declararla de forma idempotente cuando arranca el backend.

Exchanges:

| Nombre | Tipo | Durable |
| --- | --- | --- |
| `fleetops.logs.direct` | direct | si |
| `fleetops.events.topic` | topic | si |
| `fleetops.dlx.direct` | direct | si |

Colas principales:

| Cola | DLX | Dead-letter routing key |
| --- | --- | --- |
| `fleetops.logs.all.queue` | `fleetops.dlx.direct` | `fleetops.logs.all.dead` |
| `fleetops.logs.errors.queue` | `fleetops.dlx.direct` | `fleetops.logs.errors.dead` |
| `fleetops.logs.security.queue` | `fleetops.dlx.direct` | `fleetops.logs.security.dead` |

DLQ:

| DLQ | Binding en `fleetops.dlx.direct` |
| --- | --- |
| `fleetops.logs.all.dlq` | `fleetops.logs.all.dead` |
| `fleetops.logs.errors.dlq` | `fleetops.logs.errors.dead` |
| `fleetops.logs.security.dlq` | `fleetops.logs.security.dead` |

Bindings principales:

| Exchange | Cola | Routing key o patron |
| --- | --- | --- |
| `fleetops.logs.direct` | `fleetops.logs.all.queue` | `INFO` |
| `fleetops.logs.direct` | `fleetops.logs.all.queue` | `WARNING` |
| `fleetops.logs.direct` | `fleetops.logs.all.queue` | `ERROR` |
| `fleetops.logs.direct` | `fleetops.logs.errors.queue` | `ERROR` |
| `fleetops.logs.direct` | `fleetops.logs.security.queue` | `SECURITY` |
| `fleetops.events.topic` | `fleetops.logs.all.queue` | `fleetops.#` |
| `fleetops.events.topic` | `fleetops.logs.errors.queue` | `fleetops.*.error` |
| `fleetops.events.topic` | `fleetops.logs.security.queue` | `fleetops.security.#` |

## Comandos

Validar Compose:

```bash
docker compose --env-file .env.production.local -f platform/local/docker-compose.yml config
```

Levantar RabbitMQ sin eliminar volumenes existentes:

```bash
docker compose --env-file .env.production.local -f platform/local/docker-compose.yml up -d rabbitmq-1 rabbitmq-2
```

Ver contenedores:

```bash
docker compose --env-file .env.production.local -f platform/local/docker-compose.yml ps
```

Comprobar el cluster desde ambos nodos:

```bash
docker compose --env-file .env.production.local -f platform/local/docker-compose.yml exec -T rabbitmq-1 rabbitmqctl cluster_status
docker compose --env-file .env.production.local -f platform/local/docker-compose.yml exec -T rabbitmq-2 rabbitmqctl cluster_status
```

Ejecutar verificacion automatica en Windows:

```powershell
./scripts/windows/rabbitmq-cluster-check.ps1
```

Ejecutar verificacion automatica en Bash:

```bash
./scripts/unix/rabbitmq-cluster-check.sh
```

Ejecutar la demo de las tres DLQ en Windows:

```powershell
./scripts/windows/rabbitmq-dlq-demo.ps1
```

Ejecutar el backend local desde Windows con Maven Wrapper:

```powershell
./scripts/windows/local-backend-run.ps1
```

Ejecutar la demo de las tres DLQ en Bash:

```bash
./scripts/unix/rabbitmq-dlq-demo.sh
```

Ejecutar el backend local desde Bash:

```bash
./scripts/unix/local-backend-run.sh
```

Limpiar completamente el entorno:

```bash
docker compose --env-file .env.production.local -f platform/local/docker-compose.yml down -v --remove-orphans
```

## Interfaces web

- Nodo 1: `http://localhost:15672`
- Nodo 2: `http://localhost:15673`

Usa las credenciales locales definidas en `.env`. No pegues secretos reales en documentacion ni capturas.

## Prueba de DLQ

`rabbitmq-dlq-demo` usa la API HTTP de administracion para:

1. Purgar las colas de la demo.
2. Publicar un mensaje valido y confirmar que llega a la cola principal.
3. Consumir ese mensaje valido con ack.
4. Publicar un segundo mensaje.
5. Rechazarlo con `reject_requeue_false`.
6. Verificar que aparece en la DLQ correspondiente.

La prueba cubre:

- `fleetops.logs.all.queue` -> `fleetops.logs.all.dlq`
- `fleetops.logs.errors.queue` -> `fleetops.logs.errors.dlq`
- `fleetops.logs.security.queue` -> `fleetops.logs.security.dlq`

El rechazo sin requeue evita bucles infinitos y usa la configuracion `x-dead-letter-exchange` de cada cola principal.

## Backend Spring Boot

El backend conserva la topologia en `RabbitMQConfig` y lee nombres desde `application.properties`. La integracion RabbitMQ usa:

- `spring.rabbitmq.addresses`
- publisher confirms correlacionados
- publisher returns
- `mandatory=true`
- ack manual en consumidores
- `default-requeue-rejected=false`
- conversion JSON mediante `JacksonJsonMessageConverter`

Cuando el backend corre dentro de Docker, usa:

```dotenv
SPRING_RABBITMQ_ADDRESSES=rabbitmq-1:5672,rabbitmq-2:5672
```

No uses `localhost` desde un contenedor para conectarte a RabbitMQ.

Cuando el backend corre directamente desde Windows, usa:

```dotenv
SPRING_RABBITMQ_ADDRESSES=localhost:5672,localhost:5673
```

## Errores comunes

`UnknownHostException: rabbitmq-1` o `UnknownHostException: rabbitmq-2`:

- Causa: el backend corre en Windows/Maven/IntelliJ usando hostnames internos de Docker.
- Correccion: usar `SPRING_RABBITMQ_ADDRESSES=localhost:5672,localhost:5673` o ejecutar `./scripts/windows/local-backend-run.ps1`.

`ACCESS_REFUSED`:

- Causa: usuario o contrasena del cliente Spring no coinciden con las credenciales creadas en RabbitMQ.
- Correccion: revisar `SPRING_RABBITMQ_USERNAME` y `SPRING_RABBITMQ_PASSWORD`. Si se reutilizan las credenciales del broker, deben coincidir con `RABBITMQ_DEFAULT_USER` y `RABBITMQ_DEFAULT_PASS`.

Conexion rechazada en `localhost:5672`, `localhost:5673`, `127.0.0.1:15672` o `127.0.0.1:15673`:

- Causa: los contenedores no estan iniciados, el healthcheck todavia no termina, o los puertos publicados no estan disponibles.
- Correccion: ejecutar `docker compose --env-file .env -f platform/local/docker-compose.yml up -d rabbitmq-1 rabbitmq-2` y luego `./scripts/windows/rabbitmq-cluster-check.ps1`.
