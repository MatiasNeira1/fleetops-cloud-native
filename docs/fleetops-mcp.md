# FleetOps MCP

`backend/servicios/fleetops-mcp` es un adaptador Java 21 / Spring Boot 4.1.1 con Spring AI 2.0.2. Expone MCP Streamable HTTP en `/mcp`. Spring AI gestiona JSON-RPC, capacidades y sesiones; las respuestas pueden usar SSE dentro de Streamable HTTP.

## Herramientas

| Herramienta | API REST consultada | Límite |
| --- | --- | --- |
| `list_active_trips` | flota `GET /api/v1/trips` | Hasta 50 resultados; la API no pagina. |
| `get_trip` | flota `GET /api/v1/trips/{id}` | Un viaje. |
| `get_vehicle_position` | flota `GET /api/v1/vehicles/{id}/position` | Una posición. |
| `list_alerts` | ms-alerta `GET /api/v1/alerts` | Página de 1 a 50. |
| `get_alert` | ms-alerta `GET /api/v1/alerts/{id}` | Una alerta. |
| `list_trip_alerts` | ms-alerta `GET /api/v1/trips/{tripId}/alerts` | Página de 1 a 50. |

La API no ofrece un endpoint para listar conductores. Todas las herramientas son de solo lectura y sus errores no filtran cuerpos de respuesta del backend.

## Servidor local

1. Inicia `servicio-flota` y `ms-alerta` según [desarrollo local](../README.md#desarrollo-local).
2. Copia `backend/servicios/fleetops-mcp/.env.example` a `.env` en ese directorio. Reemplaza `MCP_CLIENT_TOKEN` por un valor aleatorio de al menos 32 caracteres. `.env` está ignorado por Git.
3. Desde la raíz, ejecuta `./scripts/windows/local-mcp-run.ps1`. El perfil local escucha en `http://127.0.0.1:8088/mcp` por defecto.

Un cliente MCP directo debe enviar `Authorization: Bearer <MCP_CLIENT_TOKEN>` en cada solicitud. Si incluye `Origin`, debe estar en `MCP_ALLOWED_ORIGINS` (lista separada por comas). `MCP_CLIENT_TOKEN` solo protege `/mcp` y jamás se reenvía a las API REST. `FLEETOPS_FLOTA_API_BEARER_TOKEN` y `FLEETOPS_ALERTA_API_BEARER_TOKEN` son credenciales de servicio separadas y opcionales; actualmente las API locales no validan tokens HTTP. Las URL REST y los tiempos de espera se configuran con las variables `FLEETOPS_*` del ejemplo. No hay reintentos automáticos. El token MCP compartido da acceso a todas las herramientas de lectura y no distingue usuarios.

## ChatGPT mediante Secure MCP Tunnel

El túnel crea una conexión HTTPS saliente desde `tunnel-client` hacia OpenAI. `/mcp` continúa en loopback, sin URL pública. La versión 0.0.20 de `tunnel-client` admite `MCP_EXTRA_HEADERS` y `MCP_DISCOVERY_EXTRA_HEADERS` con referencias `env:VAR`. `local-mcp-tunnel.ps1` usa esa función para añadir el bearer local a las solicitudes y sondas dirigidas a `http://127.0.0.1:8088/mcp`. El secreto queda fuera de argumentos CLI y archivos versionados.

El perfil `local` envía un keep-alive SSE cada segundo. La sonda de arranque de `tunnel-client` abre un `GET /mcp` persistente después de `initialize` y, en la versión 0.0.20, espera como máximo dos segundos a que responda. Sin keep-alive, Spring puede mantener ese GET abierto sin entregar encabezados hasta que haya un evento; el cliente informa `mcp probe timed out after 2s`. Esta configuración conserva Streamable HTTP y no cambia el timeout del túnel.

| Tramo | Credencial | Finalidad |
| --- | --- | --- |
| `tunnel-client` → control del túnel OpenAI | `CONTROL_PLANE_API_KEY` | Runtime API key de OpenAI Platform con Tunnels Read + Use. |
| `tunnel-client` → FleetOps local | `MCP_CLIENT_TOKEN` | `Authorization: Bearer ...` hacia `/mcp`. |

ChatGPT no recibe `MCP_CLIENT_TOKEN`. Selecciona **No authentication** en ChatGPT: describe la autenticación de ChatGPT hacia la conexión, mientras `/mcp` sigue protegido localmente. El acceso al túnel depende de los permisos de la organización de Platform y del espacio de ChatGPT. No selecciones OAuth para esta configuración y no uses la clave de Platform como `MCP_CLIENT_TOKEN`.

### Preparación

1. Descarga el ejecutable Windows desde [Tunnels de OpenAI Platform](https://platform.openai.com/settings/organization/tunnels) o la [última publicación oficial](https://github.com/openai/tunnel-client/releases/latest). Comprueba `tunnel-client.exe --version` y `tunnel-client.exe help quickstart`. Su ayuda debe incluir `--mcp.extra-headers` y `--mcp.discovery-extra-headers` con `env:VAR`.
2. En la página de Tunnels de Platform, crea o reutiliza un túnel privado. Copia el identificador completo `tunnel_...` a `CONTROL_PLANE_TUNNEL_ID` y **asocia el túnel al espacio de ChatGPT** donde crearás la conexión. El campo Tunnel no acepta `http://localhost:8088/mcp`.
3. Crea una runtime API key en [API keys de Platform](https://platform.openai.com/settings/organization/api-keys) cuyo principal tenga Tunnels Read + Use. Colócala en `CONTROL_PLANE_API_KEY` del entorno del proceso o del `.env` ignorado. Crear o modificar túneles requiere Tunnels Read + Manage; no uses una admin key para el proceso persistente.
4. Usa el mismo `MCP_CLIENT_TOKEN` para el servidor y el script. No lo pegues en ChatGPT.

Ambos scripts cargan el mismo `.env` ignorado; los valores no vacíos de ese archivo prevalecen sobre variables ya presentes en la consola.

Desde la raíz, con `tunnel-client.exe` en `PATH`:

```powershell
if (-not (Test-Path backend/servicios/fleetops-mcp/.env)) {
    Copy-Item backend/servicios/fleetops-mcp/.env.example backend/servicios/fleetops-mcp/.env
}
# Edita el .env ignorado: MCP_CLIENT_TOKEN, CONTROL_PLANE_TUNNEL_ID y CONTROL_PLANE_API_KEY.
./scripts/windows/local-mcp-run.ps1
```

En otra consola PowerShell:

```powershell
tunnel-client.exe --version
tunnel-client.exe help quickstart
./scripts/windows/local-mcp-tunnel.ps1 -Action Doctor
./scripts/windows/local-mcp-tunnel.ps1 -Action Run
```

Si no está en `PATH`, agrega `-TunnelClientPath 'C:\ruta\a\tunnel-client.exe'` a ambas llamadas. `Doctor` ejecuta `doctor --explain` y requiere ID y runtime key reales. La sonda de alcanzabilidad puede mostrar `HTTP 401` como `PASS`: solo confirma que el servidor respondió a esa sonda, no que se autenticó una llamada MCP. `Run` mantiene el túnel en primer plano. El panel de salud escucha en un puerto efímero de loopback; consulta la URL `/ui` que imprime al arrancar y verifica `/healthz` y `/readyz`. Para detener el túnel, pulsa `Ctrl+C` en su consola; detén el MCP en la otra consola.

Si aparece `protected resource metadata missing resource`, consulta `/health/oauth` en el panel del cliente. Con este servidor de token estático, los endpoints OAuth de descubrimiento responden 404 y el estado final esperado es `not_advertised` con `status=ok`. Esa advertencia es independiente de la sonda MCP; no añadas metadatos OAuth ficticios.

En [ChatGPT Plugins](https://chatgpt.com/#settings/Connectors), elige **Add custom MCP server** y completa:

| Campo | Valor |
| --- | --- |
| Connection | **Tunnel**; selecciona el túnel asociado al espacio o pega el `CONTROL_PLANE_TUNNEL_ID` real (`tunnel_...`). |
| Authentication | **No authentication**. |

Asigna un nombre, revisa el aviso de riesgo y crea la conexión. Mantén `tunnel-client run` activo para discovery y llamadas. Verifica `initialize`, `tools/list` y una consulta de lectura como `list_active_trips` con las API REST levantadas. Si no aparece el túnel, revisa la asociación al espacio y Tunnels Use. Si falla discovery, ejecuta `-Action Doctor` y revisa `/readyz`. Ante un 401 local, verifica que coincida `MCP_CLIENT_TOKEN`. Ante un 403 por `Origin`, identifica el origen y permite solo ese origen exacto y confiable en `MCP_ALLOWED_ORIGINS`.

El script no expone un túnel público ni modifica otros microservicios. Según `tunnel-client`, los encabezados `Authorization` enviados por el conector prevalecen sobre los estáticos; esta conexión usa **No authentication** para que el cliente inyecte el bearer local.

Referencias: [Secure MCP Tunnel](https://developers.openai.com/api/docs/guides/secure-mcp-tunnels), [onboarding de tunnel-client](https://github.com/openai/tunnel-client/blob/master/docs/onboarding.md), [configuración de encabezados MCP](https://github.com/openai/tunnel-client/blob/master/docs/configuration.md), [Spring AI MCP Server](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-streamable-http-server-boot-starter-docs.html).
