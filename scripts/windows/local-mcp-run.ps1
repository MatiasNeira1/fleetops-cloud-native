$ErrorActionPreference = "Stop"

$serviceDir = Join-Path $PSScriptRoot "..\..\backend\servicios\fleetops-mcp"
$envFile = Join-Path $serviceDir ".env"
if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Falta backend/servicios/fleetops-mcp/.env. Copia .env.example y define MCP_CLIENT_TOKEN."
}

foreach ($line in Get-Content -LiteralPath $envFile) {
    if ($line -match '^\s*([A-Z][A-Z0-9_]*)\s*=\s*(.*)$') {
        $name = $matches[1]
        $value = $matches[2].Trim().Trim('"').Trim("'")
        if ($value) { Set-Item -Path "Env:$name" -Value $value }
    }
}
if (-not $env:MCP_CLIENT_TOKEN -or $env:MCP_CLIENT_TOKEN.Length -lt 32) {
    throw "MCP_CLIENT_TOKEN debe tener al menos 32 caracteres."
}

Push-Location $serviceDir
try {
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
