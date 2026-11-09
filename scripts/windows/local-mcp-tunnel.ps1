param(
    [ValidateSet("Doctor", "Run")]
    [string]$Action = "Run",
    [string]$TunnelClientPath = ""
)

$ErrorActionPreference = "Stop"

$serviceDir = Join-Path $PSScriptRoot "..\..\backend\servicios\fleetops-mcp"
$envFile = Join-Path $serviceDir ".env"
if (Test-Path -LiteralPath $envFile) {
    foreach ($line in Get-Content -LiteralPath $envFile) {
        if ($line -match '^\s*([A-Z][A-Z0-9_]*)\s*=\s*(.*)$') {
            $name = $matches[1]
            $value = $matches[2].Trim().Trim('"').Trim("'")
            if ($value) { Set-Item -Path "Env:$name" -Value $value }
        }
    }
}

if (-not $TunnelClientPath) {
    $command = Get-Command tunnel-client -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "No se encontró tunnel-client. Descarga el binario oficial y añádelo al PATH, o indica -TunnelClientPath."
    }
    $TunnelClientPath = $command.Source
}
if (-not (Test-Path -LiteralPath $TunnelClientPath -PathType Leaf)) {
    throw "No existe el ejecutable indicado en -TunnelClientPath."
}
if (-not $env:MCP_CLIENT_TOKEN -or $env:MCP_CLIENT_TOKEN.Length -lt 32) {
    throw "MCP_CLIENT_TOKEN debe tener al menos 32 caracteres."
}
if (-not $env:CONTROL_PLANE_TUNNEL_ID -or
    $env:CONTROL_PLANE_TUNNEL_ID -eq 'tunnel_0123456789abcdef0123456789abcdef' -or
    $env:CONTROL_PLANE_TUNNEL_ID -notmatch '^tunnel_(?:[a-z0-9]{4}_)?[a-z0-9]{32}$') {
    throw "Define CONTROL_PLANE_TUNNEL_ID con el tunnel_id real de Platform."
}
if (-not $env:CONTROL_PLANE_API_KEY) {
    throw "Define CONTROL_PLANE_API_KEY con una runtime API key autorizada para Tunnels Read + Use."
}
if ($env:MCP_BIND_ADDRESS -and $env:MCP_BIND_ADDRESS -ne "127.0.0.1") {
    throw "Para el túnel local, MCP_BIND_ADDRESS debe ser 127.0.0.1."
}

$port = 8088
if ($env:MCP_PORT) {
    if (-not [int]::TryParse($env:MCP_PORT, [ref]$port) -or $port -lt 1 -or $port -gt 65535) {
        throw "MCP_PORT debe ser un puerto válido."
    }
}

# tunnel-client resuelve env:VAR al iniciar y solo envía estos encabezados al origen MCP.
$env:MCP_TUNNEL_LOCAL_AUTHORIZATION = "Bearer $($env:MCP_CLIENT_TOKEN)"
$env:MCP_EXTRA_HEADERS = "Authorization: env:MCP_TUNNEL_LOCAL_AUTHORIZATION"
$env:MCP_DISCOVERY_EXTRA_HEADERS = "Authorization: env:MCP_TUNNEL_LOCAL_AUTHORIZATION"
$env:MCP_SERVER_URL = "http://127.0.0.1:$port/mcp"
$env:HEALTH_LISTEN_ADDR = "127.0.0.1:0"

try {
    # En Windows PowerShell, tunnel-client escribe logs normales en stderr.
    # Evita que ErrorActionPreference=Stop los convierta en una excepción.
    $ErrorActionPreference = "Continue"
    if ($Action -eq "Doctor") {
        & $TunnelClientPath doctor --explain
    } else {
        & $TunnelClientPath run
    }
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Remove-Item Env:MCP_TUNNEL_LOCAL_AUTHORIZATION -ErrorAction SilentlyContinue
    Remove-Item Env:MCP_EXTRA_HEADERS -ErrorAction SilentlyContinue
    Remove-Item Env:MCP_DISCOVERY_EXTRA_HEADERS -ErrorAction SilentlyContinue
}
