# Simulador de vehículos FleetOps

El servicio `backend/servicios/fleetops-simulator` consume los viajes activos de flota por REST, envía telemetría a `POST /api/v1/telemetry` y deja que flota y `ms-alerta` creen y gestionen sus alertas. No accede a sus bases de datos. El marcador del frontend consulta de nuevo la API cada cinco segundos mientras la pestaña está visible. Las seis herramientas MCP existentes siguen siendo de lectura.

## Modelo y controles

- `SIMULATOR_ENABLED=false` al iniciar: no inicia viajes ni produce telemetría. Aun con `true`, se requiere `start` explícito.
- `POST /api/v1/simulations/demo/start` crea o reutiliza un viaje `demo-trip-*` en Santiago con vehículo `demo-vehicle-*`, combustible inicial de 15,22 % y una **línea recta simulada**, no una ruta por calles. La operación entre simulador y flota usa `FLEETOPS_SIMULATOR_SERVICE_TOKEN`.
- `GET /api/v1/simulations` y `POST /api/v1/simulations/{start,pause,resume,stop}` controlan el conjunto. Las mismas operaciones por viaje usan `/{tripId}/{action}`; `restart` recupera última posición y combustible persistidos en flota. `scenario` acepta `SPEED_LIMIT_EXCEEDED`, `STOP` o `PREPARE_LOW_FUEL`; `refuel` acepta `{"percent":10}`. Todos los controles requieren `Authorization: Bearer` con `SIMULATOR_ADMIN_TOKEN` (mínimo 32 caracteres).
- Pausa conserva el estado de la instancia. Tras reiniciar el proceso no se reanudan viajes automáticamente: el operador invoca `start` o `demo/start`; el estado de viaje, posición y combustible se reconstruye desde flota. `stop` elimina el estado en memoria; `restart` es una acción distinta.
- Las situaciones aleatorias tienen semilla, probabilidades, duración y cooldown configurables. La velocidad cambia a lo sumo `SIMULATOR_ACCELERATION_KPH_PER_SECOND` por segundo. El combustible disminuye por distancia y solo aumenta con `refuel`. Las alertas aparecen al cruzar 90 km/h y 15 % en flota; no se crean alertas por conducción normal ni se resuelven automáticamente.
- Cada simulación mantiene un evento pendiente con el mismo `eventId` hasta que flota lo acepte. Los errores de un viaje no impiden avanzar otros. RabbitMQ concede la cola exclusiva `fleetops.simulator.leader` a una sola conexión, incluso durante solapamiento de revisiones; si RabbitMQ cae, la simulación deja de producir telemetría.
- Tras `SIMULATOR_MAX_FAILURES` errores consecutivos (5 por defecto), se pausa solo el viaje afectado. El operador debe revisar la causa y usar `/{tripId}/resume` para reanudar el mismo evento pendiente.
- `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` y `/actuator/metrics` sirven para diagnóstico. Métricas: `fleetops_simulator_telemetry_accepted_total`, `fleetops_simulator_completed_total`, `fleetops_simulator_failures_total`, `fleetops_simulator_paused_after_failures_total`, `fleetops_simulator_leader_rejections_total`.

## Desarrollo local (PowerShell)

En el `.env` ignorado de la raíz configura `SIMULATOR_ADMIN_TOKEN` y `FLEETOPS_SIMULATOR_SERVICE_TOKEN` distintos de cualquier credencial MCP, de frontend o RabbitMQ. Usa los nombres de `.env.example`; no copies tokens a archivos versionados.

```powershell
$env:SIMULATOR_ENABLED='true'
docker compose --env-file .env -f platform/local/docker-compose.yml up -d --build servicio-flota ms-alerta fleetops-simulator
$settings = @{}
Get-Content .env | ForEach-Object { if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') { $settings[$matches[1]]=$matches[2] } }
$headers = @{ Authorization = 'Bearer ' + $settings.SIMULATOR_ADMIN_TOKEN }
$status = Invoke-RestMethod -Method Post -Headers $headers -Uri http://127.0.0.1:8089/api/v1/simulations/demo/start
$tripId = $status.trips[0].tripId
Invoke-RestMethod -Method Post -Headers $headers -ContentType application/json -Body '{"scenario":"SPEED_LIMIT_EXCEEDED"}' -Uri "http://127.0.0.1:8089/api/v1/simulations/$tripId/scenario"
Invoke-RestMethod -Headers $headers -Uri http://127.0.0.1:8089/api/v1/simulations
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/v1/trips/$tripId"
Invoke-RestMethod -Uri "http://127.0.0.1:8081/api/v1/trips/$tripId/alerts"
Invoke-RestMethod -Method Post -Headers $headers -Uri http://127.0.0.1:8089/api/v1/simulations/pause
```

Los controles `resume`, `stop`, `/{tripId}/pause`, `/{tripId}/resume`, `/{tripId}/stop`, `/{tripId}/restart` usan la misma cabecera. Para preparar combustible bajo después de recuperar velocidad usa `{"scenario":"PREPARE_LOW_FUEL"}`; el valor baja con la distancia. Para una parada usa `{"scenario":"STOP"}`. La URL del simulador local está enlazada solo a `127.0.0.1` en Docker Compose.

## Azure

Recursos existentes confirmados: suscripción `cb7d7424-2923-42e4-9fa3-b2659b7890a7`, grupo `rg-municipalidad`, entorno `cae-fleetops-dev`, ACR `acrmunicipalidaddoc`, apps `ca-fleetops-servicio-flota`, `ca-fleetops-ms-alerta`, `ca-fleetops-mcp` y almacenamiento de estado `rg-fleetops-dev/stfleetopsweb/tfstate-mcp`. El módulo crea únicamente `ca-fleetops-simulator`, `id-fleetops-simulator` y su rol `AcrPull`. La nueva app carece de ingreso; los controles se invocan con `az containerapp exec` en el contenedor autorizado. Usa 0,5 vCPU, 1 GiB y una réplica mínima y máxima. Los secretos se guardan como secretos de Container Apps y en el estado Terraform privado; restringe el acceso a ambos.

Publica las tres imágenes con `az acr build` y consulta sus digests sin usar etiquetas mutables para el despliegue:

```powershell
az login
az account set --subscription cb7d7424-2923-42e4-9fa3-b2659b7890a7
az acr build --registry acrmunicipalidaddoc --image fleetops/servicio-flota:sim-20261108-1 backend/servicios/servicio-flota
az acr build --registry acrmunicipalidaddoc --image fleetops/ms-alerta:sim-20261108-1 backend/servicios/ms-alerta
az acr build --registry acrmunicipalidaddoc --image fleetops/fleetops-simulator:sim-20261108-1 backend/servicios/fleetops-simulator
$flotaDigest = az acr repository show -n acrmunicipalidaddoc --image fleetops/servicio-flota:sim-20261108-1 --query digest -o tsv
$alertaDigest = az acr repository show -n acrmunicipalidaddoc --image fleetops/ms-alerta:sim-20261108-1 --query digest -o tsv
$simulatorDigest = az acr repository show -n acrmunicipalidaddoc --image fleetops/fleetops-simulator:sim-20261108-1 --query digest -o tsv
$flotaImage = "acrmunicipalidaddoc.azurecr.io/fleetops/servicio-flota@$flotaDigest"
$alertaImage = "acrmunicipalidaddoc.azurecr.io/fleetops/ms-alerta@$alertaDigest"
$simulatorImage = "acrmunicipalidaddoc.azurecr.io/fleetops/fleetops-simulator@$simulatorDigest"
$terraform = Join-Path $env:TEMP 'fleetops-terraform/terraform.exe'
./scripts/windows/azure-simulator-terraform.ps1 -Action Plan -SimulatorImage $simulatorImage -FlotaImage $flotaImage -AlertaImage $alertaImage -TerraformPath $terraform
./scripts/windows/azure-simulator-terraform.ps1 -Action Apply -SimulatorImage $simulatorImage -FlotaImage $flotaImage -AlertaImage $alertaImage -TerraformPath $terraform
```

El script obtiene en memoria la dirección, usuario y secreto RabbitMQ ya configurados en flota. Preserva los secretos existentes al añadir el token de servicio a flota. No imprime los valores. Aplica primero flota y `ms-alerta` para que acepten y consuman telemetría, y crea el simulador con `SIMULATOR_ENABLED=false`. Si faltan permisos ARM `listSecrets`, el script se detiene antes de modificar recursos. El estado Terraform requiere acceso Azure RBAC al blob privado.

Para habilitar demostraciones, repite `Plan` y `Apply` con `-SimulatorEnabled`. No se inicia ningún viaje al habilitar la app. Dentro de Azure:

```powershell
az containerapp exec -g rg-municipalidad -n ca-fleetops-simulator --container fleetops-simulator --command 'sh /app/simctl.sh status'
az containerapp exec -g rg-municipalidad -n ca-fleetops-simulator --container fleetops-simulator --command 'sh /app/simctl.sh demo'
az containerapp exec -g rg-municipalidad -n ca-fleetops-simulator --container fleetops-simulator --command 'sh /app/simctl.sh speed demo-trip-REEMPLAZAR_ID'
az containerapp exec -g rg-municipalidad -n ca-fleetops-simulator --container fleetops-simulator --command 'sh /app/simctl.sh pause'
az containerapp exec -g rg-municipalidad -n ca-fleetops-simulator --container fleetops-simulator --command 'sh /app/simctl.sh resume'
az containerapp exec -g rg-municipalidad -n ca-fleetops-simulator --container fleetops-simulator --command 'sh /app/simctl.sh stop'
```

`simctl.sh` lee el token administrativo desde la variable del contenedor y pasa la cabecera a `curl` por entrada estándar; el secreto no aparece en el argumento CLI. Para detener **por completo** la Container App al terminar, ejecuta `./scripts/windows/azure-simulator-power.ps1 -Action Stop`; `-Action Start` la vuelve a iniciar. Pausar viajes conserva progreso pero deja una réplica ejecutándose y puede consumir cómputo. La VM RabbitMQ, PostgreSQL, ACR, almacenamiento, logs y las otras Container Apps tienen su propio consumo; consulta el costo real en Azure Cost Management. Las imágenes nuevas también consumen almacenamiento del ACR existente.

El workflow `.github/workflows/fleetops-simulator.yml` verifica los tres servicios, frontend y Terraform. El despliegue manual usa OIDC en `fleetops-dev` con `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`, `SIMULATOR_ADMIN_TOKEN` y `FLEETOPS_SIMULATOR_SERVICE_TOKEN` como secretos de GitHub. El parámetro `simulator_enabled` parte en `false`. `publish_frontend` permite publicar el sitio estático cuando la identidad federada tiene `Storage Blob Data Contributor` sobre `stfleetopsweb`; configura en `fleetops-dev` las siete variables `VITE_*` descritas en `frontend/.env.example`, usando las URLs y IDs del frontend existente.

Para revertir, primero pausa o detén el simulador y la Container App. Las imágenes anteriores a este despliegue eran `acrmunicipalidaddoc.azurecr.io/fleetops/servicio-flota@sha256:ffdfd09676b0bbd197e4423bf19b72f59b76ac2bb068585bcb8b58926a7a5d44` y `acrmunicipalidaddoc.azurecr.io/fleetops/ms-alerta@sha256:1137112dfd775476fbb840e52efab3652d0ccaa10d084db72231dc48bd53f6a0`. Usa `az containerapp update -g rg-municipalidad -n <app> --image <digest-anterior>` para cada app. Revierte el frontend reconstruyendo la revisión Git anterior con la misma configuración de producción; el almacenamiento estático no tiene versionado activo de `index.html`. El estado de flota no se borra; las filas `demo-*` quedan identificadas como simuladas. No destruyas la infraestructura Terraform si quieres conservar auditoría y evitar tocar recursos de otros servicios.

## Verificación realizada el 8 de noviembre de 2026

| Entorno | Evidencia |
| --- | --- |
| Local | 48 pruebas de flota, 9 del simulador y 21 del frontend pasaron; `ms-alerta` compiló y sus pruebas pasaron en Docker y ACR. Un viaje demo recorrió la línea simulada, superó 90 km/h, recuperó velocidad, bajó de 15 % y terminó. Flota creó dos alertas y `ms-alerta` recibió ambas. Una segunda instancia intentó iniciar el mismo demo y recibió HTTP 409 por el lease RabbitMQ. |
| Azure | Terraform aplicó 3 altas, 0 cambios y 0 bajas en el despliegue inicial. El simulador arrancó desactivado, sin ingreso, con una réplica lista; la API de control respondió 401 sin bearer y 200 con bearer. Flota y `ms-alerta` respondieron 200. El sitio estático publicó el nuevo bundle y sirvió `index.html` con HTTP 200. |
| Demo Azure | `demo-trip-341c4c3f-a734-4c2b-81a6-91739a5b6f7a` llegó a 105 km/h, luego recuperó 65 km/h, bajó a 14,99 % de combustible y terminó con 0 km/h. Flota creó `SPEEDING` y `LOW_FUEL`; `ms-alerta` recibió `SPEED_LIMIT_EXCEEDED` y `LOW_FUEL`, una vez cada una. |
| MCP desde ChatGPT | Las herramientas reales `get_trip`, `get_vehicle_position` y `list_trip_alerts` devolvieron el viaje demo, su posición final y las dos alertas de `ms-alerta`. Las seis herramientas existentes siguen siendo de lectura. |
| Recuperación Azure | Se reinició la revisión durante `demo-trip-3b78dd95-efab-4b56-be9f-98a788a4eec4`. La instancia nueva retomó el mismo viaje desde ~0,98 km y 15,06 % persistidos; avanzó al 75 % y luego completó. Durante el solapamiento se observaron dos réplicas, luego una. El viaje produjo una sola alerta `LOW_FUEL`. |
| Detención y estado final | El script `azure-simulator-power.ps1` llevó `runningStatus` a `Stopped` y cero réplicas; `Start` volvió a poner la app en `Running`. La revisión final `ca-fleetops-simulator--0000003` quedó sana, con una réplica, sin ingreso y con `globalPaused=true`, `trips=[]`. El último plan Terraform informó `No changes`. |

La UI del navegador no pudo inspeccionarse porque el host de acceso al navegador no estuvo disponible. La publicación del bundle y las respuestas REST/MCP sí se comprobaron; el movimiento visual del marcador queda pendiente de una sesión de navegador autenticada. `az containerapp exec` respondió ocasionalmente HTTP 429 con `Retry-After: 600`; se esperó el plazo indicado antes de repetir las acciones. La réplica mínima de 1 sigue ejecutándose aunque la simulación esté pausada; usa el script `Stop` cuando no necesites disponibilidad del servicio.

Fuentes oficiales: [colas exclusivas RabbitMQ](https://www.rabbitmq.com/docs/queues), [secretos de Container Apps](https://learn.microsoft.com/en-us/azure/container-apps/manage-secrets), [actualización de variables](https://learn.microsoft.com/en-us/azure/container-apps/environment-variables), [ingreso interno](https://learn.microsoft.com/en-us/azure/container-apps/ingress), [escalado](https://learn.microsoft.com/en-us/azure/container-apps/scale-app), [facturación](https://learn.microsoft.com/en-us/azure/container-apps/billing), [operación Stop](https://learn.microsoft.com/en-us/rest/api/resource-manager/containerapps/container-apps/stop?view=rest-resource-manager-containerapps-2025-07-01).
