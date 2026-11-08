# FleetOps MCP en Azure

Esta implementación usa la Container Apps Environment existente `cae-fleetops-dev` en `rg-municipalidad`. La nueva app `ca-fleetops-mcp` ejecuta dos contenedores en la misma réplica: Spring Boot en `127.0.0.1:8088` y `tunnel-client` 0.0.20. No tiene ingreso público. El cliente del túnel conecta por salida a OpenAI y añade `Authorization: Bearer` a las solicitudes locales usando `MCP_EXTRA_HEADERS` y `MCP_DISCOVERY_EXTRA_HEADERS`.

El Tunnel ID es `tunnel_6af0a5b4c3e88191a7fe55129b59b8c5`. El perfil `prod` envía keep-alive SSE cada segundo para que la sonda de la versión fijada del cliente reciba datos antes de su límite de dos segundos. Se conservan las seis herramientas de lectura descritas en [FleetOps MCP](fleetops-mcp.md).

## Recursos y dependencias

| Recurso | Uso |
| --- | --- |
| `rg-municipalidad/cae-fleetops-dev` | Entorno de Container Apps reutilizado. |
| `rg-municipalidad/acrmunicipalidaddoc` | Registro de la imagen MCP, fijada por digest SHA-256. |
| `rg-municipalidad/ca-fleetops-servicio-flota` | REST en `http://ca-fleetops-servicio-flota` dentro del mismo entorno, rutas `/api/v1/trips` y `/api/v1/vehicles/{id}/position`. |
| `rg-municipalidad/ca-fleetops-ms-alerta` | REST en `http://ca-fleetops-ms-alerta`, rutas `/api/v1/alerts` y `/api/v1/trips/{tripId}/alerts`. |
| `rg-fleetops-dev/stfleetopsweb` | Estado Terraform en el contenedor privado `tfstate-mcp`. |
| `rg-municipalidad/id-fleetops-mcp` | Nueva identidad con permiso `AcrPull` únicamente sobre el registro existente. |
| `rg-municipalidad/vm-fleetops-rabbitmq` | Dependencia actual de base de datos y RabbitMQ de las apps REST; debe estar encendida para que las consultas funcionen. |

Las API desplegadas se consultan por los nombres internos de Container Apps. El MCP usa los endpoints REST existentes. El código actual de esos servicios no comprueba un bearer HTTP; si se habilita tal protección, configura credenciales **de servicio** distintas en `FLEETOPS_FLOTA_API_BEARER_TOKEN` y `FLEETOPS_ALERTA_API_BEARER_TOKEN`. No reutilices el token del frontend ni `MCP_CLIENT_TOKEN`. El tráfico interno usa HTTP en el entorno de Container Apps; revisa ese límite de confianza antes de trasladar la solución a producción.

En la inspección previa al despliegue, ambas Container Apps REST estaban detenidas y la VM estaba desasignada. Se iniciaron para esta instalación y las dos rutas de lectura respondieron HTTP 200 sin bearer HTTP el 8 de noviembre de 2026.

La réplica mínima y máxima de la nueva app es una. Esto mantiene el túnel activo cuando el computador local está apagado y genera consumo continuo de CPU/memoria en Container Apps. También pueden generar costo continuo la VM encendida, el registro, el almacenamiento del estado, los logs y el tráfico saliente. Consulta los precios vigentes de tu región y los límites de Azure for Students; aquí no se asume un importe.

## Preparación y despliegue desde Windows

Requiere sesión `az login` en la suscripción que contiene los recursos, Docker o `az acr build`, Terraform 1.17.1 y permisos para crear la Container App, identidad, asignación `AcrPull` y contenedor de estado. El archivo `backend/servicios/fleetops-mcp/.env` es local e ignorado por Git. Contiene `MCP_CLIENT_TOKEN`, `CONTROL_PLANE_API_KEY` y el Tunnel ID real; **no** copies los valores a comandos, `tfvars` ni la documentación. La runtime API key de OpenAI Platform necesita `Tunnels Read + Use` sobre el túnel existente.

Desde la raíz del repositorio:

```powershell
az account show --query '{subscription:id,name:name}' -o json
az storage container create --account-name stfleetopsweb --name tfstate-mcp --auth-mode login --public-access off
az acr build --registry acrmunicipalidaddoc --image fleetops/fleetops-mcp:v0.0.1-azure-20261108 backend/servicios/fleetops-mcp
$digest = az acr repository show --name acrmunicipalidaddoc --image fleetops/fleetops-mcp:v0.0.1-azure-20261108 --query digest -o tsv
$image = "acrmunicipalidaddoc.azurecr.io/fleetops/fleetops-mcp@$digest"
./scripts/windows/azure-mcp-terraform.ps1 -Action Plan -McpImage $image
```

Revisa que el plan cree únicamente `id-fleetops-mcp`, su asignación `AcrPull` y `ca-fleetops-mcp`. Terraform lee los secretos desde el `.env` ignorado y los guarda como secretos de Container Apps. El estado remoto contiene material sensible y queda en un contenedor privado del Storage Account existente; limita los permisos de Blob Data a los operadores de despliegue. Terraform puede mostrar cambios de secretos como `(sensitive value)`; evita guardar planes o salida detallada en artefactos públicos.

Para completar el despliegue:

```powershell
az vm start --resource-group rg-municipalidad --name vm-fleetops-rabbitmq
$base = 'https://management.azure.com/subscriptions/cb7d7424-2923-42e4-9fa3-b2659b7890a7/resourceGroups/rg-municipalidad/providers/Microsoft.App/containerApps'
az rest --method post --url "$base/ca-fleetops-servicio-flota/start?api-version=2025-07-01" --output none
az rest --method post --url "$base/ca-fleetops-ms-alerta/start?api-version=2025-07-01" --output none
./scripts/windows/azure-mcp-terraform.ps1 -Action Apply -McpImage $image
az containerapp show --resource-group rg-municipalidad --name ca-fleetops-mcp --query '{running:properties.runningStatus,ingress:properties.configuration.ingress,min:properties.template.scale.minReplicas,max:properties.template.scale.maxReplicas}' -o json
```

`Apply` calcula el plan actualizado y solicita confirmación interactiva. Para automatización supervisada, el script también admite `-AutoApprove`. Esta versión de Azure CLI no ofrece `az containerapp start`, por eso se usa la API REST oficial mediante `az rest`. La aplicación no tiene ingreso, por lo que `/mcp` no obtiene una URL pública. Los secretos se inyectan como variables de entorno en la réplica y el sidecar compone el bearer dentro del contenedor, sin pasarlo como argumento de proceso u opción CLI. No ejecutes el túnel local y el de Azure simultáneamente con el mismo Tunnel ID durante la conmutación.

## CI/CD

`.github/workflows/fleetops-mcp.yml` compila y ejecuta las pruebas en cambios MCP/Terraform. El despliegue se activa manualmente con `workflow_dispatch` y `deploy=true`. Usa `azure/login@v3` con OIDC y el environment de GitHub `fleetops-dev`. Prepara una identidad federada con acceso limitado a los recursos FleetOps y almacena en ese environment:

- `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID` para OIDC.
- `MCP_CLIENT_TOKEN`, `CONTROL_PLANE_API_KEY` como secretos. No uses una admin key del túnel.

La identidad necesita permisos para ACR build/push, leer los recursos existentes, crear la app/identidad/asignación de rol y leer/escribir blobs en `tfstate-mcp`. Configura una credencial federada para el subject `repo:<propietario>/<repositorio>:environment:fleetops-dev` en Entra ID. El flujo no crea ni cambia otros proyectos. Antes de ejecutar el workflow, comprueba el alcance de sus roles y que el environment tenga las restricciones de rama requeridas.

## Verificación y diagnóstico

1. Comprueba que las tres dependencias estén en ejecución. Consulta directamente `GET /api/v1/trips` y `GET /api/v1/alerts`; registra código HTTP y tiempo sin imprimir secretos. Si el resultado es 401/403, verifica el mecanismo de autenticación de esas API y añade credenciales de servicio autorizadas al MCP.
2. Comprueba que `ca-fleetops-mcp` tenga una réplica lista y que la configuración de ingreso sea `null`. Revisa logs de la app con `az containerapp logs show -g rg-municipalidad -n ca-fleetops-mcp --container tunnel-client --follow` y repite para `fleetops-mcp`. Los logs no deben incluir tokens.
3. En el log del túnel, confirma que la sonda MCP completa `initialize`, que `/readyz` queda listo y que la conexión de control permanece activa. La advertencia `protected resource metadata missing resource` se investiga aparte; con autenticación estática no se publican metadatos OAuth ficticios.
4. En ChatGPT, configura **Connection: Tunnel** con el Tunnel ID existente y **Authentication: No authentication**. Ejecuta `tools/list` y una herramienta de lectura. La conexión solo se considera completa después de una llamada real desde ChatGPT que alcance las API desplegadas.

El servicio MCP carece de ingreso; una comprobación directa autenticada se puede hacer en una red de prueba o con una réplica local de la imagen. Una petición sin bearer a `/mcp` debe devolver 401. No actives ingreso público solo para probarlo.

### Resultado observado el 8 de noviembre de 2026

- Imagen MCP compilada en ACR y fijada por digest; cinco pruebas Maven aprobadas.
- Terraform aplicó tres recursos nuevos, sin cambios ni eliminaciones de recursos existentes.
- El plan posterior quedó en `No changes` tras declarar explícitamente el perfil `Consumption` que Azure devuelve por defecto.
- `ca-fleetops-mcp` quedó en estado `Running`, con `ingress: null`, una réplica y ambos contenedores listos sin reinicios.
- La sonda del túnel inicializó una sesión MCP y arrancó el poller; los logs registraron comandos de ChatGPT reenviados al MCP. La advertencia de descubrimiento OAuth persiste porque este servidor usa un token estático; no bloqueó las llamadas de herramientas.
- Desde el sidecar, `/mcp` sin bearer devolvió HTTP 401. La imagen local con perfil `prod` completó `initialize` (200), `notifications/initialized` (202) y `tools/list` (200, seis herramientas); el GET SSE respondió en menos de dos segundos.
- Las API desplegadas devolvieron HTTP 200 en `GET /api/v1/trips` y `GET /api/v1/alerts`. Desde el conector FleetOps de ChatGPT se invocaron `list_active_trips` y `list_alerts` sin error; la segunda devolvió una página válida vacía. Esas llamadas llegaron a la instancia de Azure con el túnel local detenido.
- El workflow de GitHub está preparado, pero su despliegue automatizado depende de configurar la identidad federada y los secretos del environment `fleetops-dev`, y de subir estos archivos al repositorio.

## Reversión y túnel local

Para revertir la conmutación, detén `ca-fleetops-mcp` con `az rest --method post --url "$base/ca-fleetops-mcp/stop?api-version=2025-07-01" --output none` y vuelve a ejecutar `./scripts/windows/local-mcp-tunnel.ps1 -Action Run` después de levantar el MCP local. Para retirar **solo** los recursos nuevos de este módulo, revisa primero `terraform plan -destroy`; `terraform destroy` quita la app MCP, su identidad y la asignación `AcrPull`, pero no los recursos existentes consultados como data sources. La imagen en ACR, el contenedor de estado y las dependencias REST se administran aparte. No detengas la VM ni las API si otros consumidores las usan.

Puedes detener el túnel local ahora: la réplica de Azure está lista y una llamada de lectura desde ChatGPT llegó a las API desplegadas. Mantén los scripts locales como vía de recuperación, pero evita mantener ambos procesos conectados a la vez.

Referencias: [Secure MCP Tunnels](https://developers.openai.com/api/docs/guides/secure-mcp-tunnels), [Docker de tunnel-client](https://github.com/openai/tunnel-client/blob/master/docs/deployment/docker.md), [varios contenedores en Container Apps](https://learn.microsoft.com/en-us/azure/container-apps/containers), [comunicación interna entre apps](https://learn.microsoft.com/en-us/azure/container-apps/connect-apps), [facturación de Container Apps](https://learn.microsoft.com/en-us/azure/container-apps/billing).
