# FleetOps

FleetOps es una plataforma Cloud Native de gestión logística y de flotas, diseñada como monorepo para construir progresivamente una arquitectura distribuida, orientada a eventos y desplegable en Microsoft Azure.

## Objetivo del proyecto

El objetivo de FleetOps es demostrar prácticas modernas de ingeniería para una plataforma logística, incluyendo:

- Arquitectura distribuida.
- Microservicios.
- Event-driven architecture.
- DevOps.
- DevSecOps.
- Observabilidad.
- Resiliencia.
- Microsoft Azure.

## Estado del proyecto

- ✅ Engineering Foundation
- ⬜ Backend Core
- ⬜ Frontend
- ⬜ Event-Driven
- ⬜ Infraestructura Azure
- ⬜ CI/CD
- ⬜ DevSecOps
- ⬜ Observabilidad
- ⬜ Performance / k6

## Arquitectura objetivo

Tecnologías objetivo del proyecto:

- Backend: Java + Spring Boot.
- Frontend: React + TypeScript.
- Cloud: Microsoft Azure.
- Mensajería: Azure Event Hubs y Azure Service Bus.
- Datos: PostgreSQL.
- IaC: Terraform.
- CI/CD: GitHub Actions.
- Observabilidad: Azure Monitor, Application Insights y Log Analytics.

Estas tecnologías representan la arquitectura objetivo. No todos los componentes están implementados todavía.

## Estructura del repositorio

- `backend/`: futuros microservicios backend.
- `frontend/`: panel web de FleetOps.
- `backend/servicios/fleetops-simulator/`: simulación controlada de telemetría; consulta [guía local y Azure](docs/fleetops-simulator.md).
- `infra/`: infraestructura como código.
- `contracts/`: contratos OpenAPI, AsyncAPI y schemas.
- `docs/`: documentación técnica, arquitectura, ADRs, runbooks y threat model.
- `tests/`: pruebas transversales del monorepo.
- `load-tests/`: pruebas de carga y performance con k6.
- `platform/`: configuración local y soporte de plataforma.
- `scripts/`: scripts de desarrollo para Windows y Unix.

## Ambientes

FleetOps define inicialmente tres ambientes:

- `LOCAL`: entorno del desarrollador con Docker Compose.
- `DEV`: integración cloud en Azure con recursos mínimos.
- `DEMO`: ambiente estable para portafolio y entrevistas.

Más detalle en [docs/environments.md](docs/environments.md).

La configuración local de RabbitMQ en clúster de dos nodos está documentada en [docs/rabbitmq-local.md](docs/rabbitmq-local.md).
El procesamiento asíncrono de alertas con `ms-alerta` está documentado en [docs/ms-alerta-rabbitmq.md](docs/ms-alerta-rabbitmq.md).
El servidor MCP de solo lectura está documentado en [docs/fleetops-mcp.md](docs/fleetops-mcp.md). Su despliegue privado en Azure se describe en [docs/fleetops-mcp-azure.md](docs/fleetops-mcp-azure.md).
La imagen Docker del backend `servicio-flota` está documentada en [docs/backend-docker.md](docs/backend-docker.md).

## Documentación OpenAPI

Cada API publica su contrato generado por Springdoc y una interfaz Swagger UI:

| Servicio | Swagger UI | OpenAPI JSON |
| --- | --- | --- |
| Flota (puerto 8080) | `http://localhost:8080/swagger-ui/index.html` | `http://localhost:8080/v3/api-docs` |
| Alertas (puerto 8081) | `http://localhost:8081/swagger-ui/index.html` | `http://localhost:8081/v3/api-docs` |

El contrato vivo refleja los controladores y distingue las dos APIs, que tienen rutas de alertas superpuestas. [`fleetops-openapi.json`](fleetops-openapi.json) y [`ms-alerta-openapi.json`](ms-alerta-openapi.json) son capturas locales de ambos contratos; para integraciones usa el JSON servido por la instancia que estés ejecutando.

## Desarrollo local

1. Copia el archivo de ejemplo:

   ```bash
   cp .env.example .env
   ```

2. Ajusta los valores locales si corresponde.

3. Levanta PostgreSQL, RabbitMQ y ambas APIs locales.

   Windows:

   ```powershell
   ./scripts/windows/local-up.ps1
   ```

   Unix:

   ```bash
   ./scripts/unix/local-up.sh
   ```

   También puedes usar:

   ```bash
   make local-up
   ```

4. Comprueba los contenedores:

   ```bash
   make local-ps
   ```

5. Inicia el frontend en otra terminal:

   ```powershell
   cd frontend
   Copy-Item .env.example .env.local
   # En .env.local, configura VITE_API_BASE_URL=http://localhost:8080
   npm install
   npm run dev
   ```

   El proxy de Vite usa `VITE_API_BASE_URL` como destino; en local configúralo con `http://localhost:8080`. El inicio de sesión del frontend requiere una aplicación Entra configurada para la URI de redirección local.

6. Detén el entorno:

   ```bash
   make local-down
   ```

## Flujo Git

El flujo de trabajo, la convención de commits, nombres de ramas y proceso de Pull Requests están documentados en [CONTRIBUTING.md](CONTRIBUTING.md).

## Licencia

MIT.
