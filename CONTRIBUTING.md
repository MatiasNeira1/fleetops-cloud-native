# Guía de contribución

Esta guía define los estándares iniciales de colaboración para FleetOps. El objetivo es mantener el monorepo simple, revisable y preparado para crecer con microservicios, infraestructura y aplicaciones frontend sin introducir herramientas innecesarias antes de tiempo.

## 1. Flujo de trabajo Git

FleetOps utiliza GitHub Flow / trunk-based development ligero.

La rama principal es `main`.

No se utilizarán ramas permanentes `develop`, `release` ni Git Flow tradicional.

Flujo esperado:

```text
main
  ↓
crear rama
  ↓
desarrollo
  ↓
tests
  ↓
push
  ↓
Pull Request
  ↓
CI
  ↓
review
  ↓
merge a main
  ↓
eliminar rama
```

Una vez configurada la protección de rama, no se permiten pushes directos a `main`. Todo cambio debe entrar mediante Pull Request.

## 2. Convención de commits

Los commits deben seguir Conventional Commits.

Formato:

```text
<tipo>(<scope>): descripción
```

Tipos permitidos:

- `feat`: nueva funcionalidad.
- `fix`: corrección de errores.
- `docs`: cambios de documentación.
- `test`: pruebas.
- `refactor`: refactorización sin cambio funcional.
- `perf`: mejora de rendimiento.
- `chore`: tareas de mantenimiento.
- `ci`: cambios de integración continua.
- `build`: cambios de build, empaquetado o dependencias de construcción.
- `security`: cambios relacionados con seguridad.
- `revert`: reversión de cambios.

Ejemplos:

```text
chore(repo): agregar configuracion base del monorepo
feat(flota): implementar registro de vehiculos
feat(telemetria): publicar eventos en Event Hubs
fix(alertas): evitar procesamiento duplicado de eventos
test(flota): agregar pruebas de integracion
ci(backend): agregar pipeline de validacion
security(ci): integrar escaneo con Trivy
docs(architecture): documentar estrategia de mensajeria
```

Por ahora la convención queda documentada sin agregar Husky, commitlint ni dependencias Node en la raíz del repositorio.

## 3. Convención para nombres de ramas

Las ramas deben ser pequeñas, de corta duración y partir desde `main`.

Prefijos permitidos:

- `feature/<descripcion>`
- `fix/<descripcion>`
- `docs/<descripcion>`
- `refactor/<descripcion>`
- `chore/<descripcion>`
- `ci/<descripcion>`
- `security/<descripcion>`

Ejemplos:

```text
feature/gestion-vehiculos
feature/telemetria-event-hubs
fix/idempotencia-alertas
docs/adr-service-bus
ci/backend-pipeline
security/gitleaks
```

Usa descripciones breves, en minúsculas y separadas por guiones.

## 4. Proceso para Pull Requests

Todo Pull Request debe:

1. Tener un alcance claro y acotado.
2. Explicar qué cambia y por qué.
3. Referenciar el issue relacionado cuando exista.
4. Incluir evidencia de validación cuando corresponda.
5. Pasar los checks de CI disponibles.
6. Ser revisado por los owners definidos en `CODEOWNERS`.
7. Tener conversaciones resueltas antes del merge.

Cuando los pipelines estén disponibles, algunos checks podrán pasar a ser obligatorios para mergear a `main`.

## 5. Requisitos mínimos antes de abrir PR

Antes de solicitar revisión:

- Verifica que el código o documentación cambien únicamente lo necesario.
- Ejecuta los tests relevantes para el área modificada.
- Revisa el diff completo localmente.
- Confirma que no agregaste secretos, credenciales ni archivos locales del IDE.
- Actualiza contratos, documentación o runbooks si el cambio lo requiere.
- Documenta nuevas variables de entorno en `.env.example`.

## 6. Mantener Pull Requests pequeños

Preferimos PRs pequeños y frecuentes.

Recomendaciones:

- Evita mezclar refactors grandes con features.
- Separa cambios de infraestructura, backend, frontend y documentación cuando sea razonable.
- Si un cambio crece demasiado, divídelo en PRs secuenciales.
- Mantén cada PR fácil de revisar en una sola sesión.

## 7. Política de no subir secretos

Está prohibido subir secretos al repositorio.

No se deben versionar:

- Tokens de acceso.
- Claves privadas.
- Certificados privados.
- Cadenas de conexión reales.
- Credenciales de Azure, GitHub, bases de datos o servicios externos.
- Archivos `.env` reales.

Si un secreto se sube por error, debe considerarse comprometido y rotarse inmediatamente.

## 8. Variables de entorno y `.env.example`

Toda variable de entorno necesaria debe documentarse en `.env.example`.

Reglas:

- `.env.example` puede versionarse.
- `.env`, `.env.local` y otros archivos `.env.*` reales no deben versionarse.
- Los valores de `.env.example` deben ser placeholders seguros, nunca credenciales reales.

## 9. CODEOWNERS

El archivo `.github/CODEOWNERS` define los responsables iniciales de revisión por área del monorepo.

Actualmente existe un único owner, pero la estructura está preparada para evolucionar hacia equipos independientes por dominio, plataforma, frontend, backend, infraestructura, seguridad y QA.
