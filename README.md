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
- `frontend/`: futura aplicación web.
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
La imagen Docker del backend `servicio-flota` está documentada en [docs/backend-docker.md](docs/backend-docker.md).

## Desarrollo local

1. Copia el archivo de ejemplo:

   ```bash
   cp .env.example .env
   ```

2. Ajusta los valores locales si corresponde.

3. Levanta la infraestructura local.

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

5. Ejecuta backend y frontend localmente:

   ```powershell
   ./scripts/windows/local-backend-run.ps1
   ```

   En otra terminal:

   ```powershell
   cd frontend
   Copy-Item .env.example .env.local
   npm run dev
   ```

   `frontend/.env.example` define `VITE_API_BASE_URL`. En desarrollo local puede quedar vacío para usar el proxy de Vite hacia `http://localhost:8080`. Si quieres llamar directo al backend, usa `VITE_API_BASE_URL=http://localhost:8080`.

6. Detén el entorno:

   ```bash
   make local-down
   ```

## Flujo Git

El flujo de trabajo, la convención de commits, nombres de ramas y proceso de Pull Requests están documentados en [CONTRIBUTING.md](CONTRIBUTING.md).

## Licencia

MIT.
