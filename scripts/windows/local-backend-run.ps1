$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$EnvFile = Join-Path $RepoRoot ".env"
$ServiceDir = Join-Path $RepoRoot "backend\servicios\servicio-flota"
$MavenWrapper = Join-Path $ServiceDir "mvnw.cmd"

function Read-DotEnvValue {
    param(
        [string] $Path,
        [string] $Name
    )

    $line = Get-Content -LiteralPath $Path |
        Where-Object { $_ -match "^\s*$([regex]::Escape($Name))\s*=" } |
        Select-Object -First 1

    if (-not $line) {
        return $null
    }

    return ($line -split "=", 2)[1].Trim().Trim('"').Trim("'")
}

if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw "Falta .env en la raiz del repositorio. Copia .env.example a .env y define las variables RabbitMQ locales."
}

if (-not (Test-Path -LiteralPath $MavenWrapper)) {
    throw "No se encontro Maven Wrapper en $MavenWrapper"
}

$rabbitUser = Read-DotEnvValue $EnvFile "SPRING_RABBITMQ_USERNAME"
if ([string]::IsNullOrWhiteSpace($rabbitUser)) {
    $rabbitUser = Read-DotEnvValue $EnvFile "RABBITMQ_DEFAULT_USER"
}

$rabbitPass = Read-DotEnvValue $EnvFile "SPRING_RABBITMQ_PASSWORD"
if ([string]::IsNullOrWhiteSpace($rabbitPass)) {
    $rabbitPass = Read-DotEnvValue $EnvFile "RABBITMQ_DEFAULT_PASS"
}

if ([string]::IsNullOrWhiteSpace($rabbitUser)) {
    throw "Falta SPRING_RABBITMQ_USERNAME o RABBITMQ_DEFAULT_USER en .env"
}

if ([string]::IsNullOrWhiteSpace($rabbitPass)) {
    throw "Falta SPRING_RABBITMQ_PASSWORD o RABBITMQ_DEFAULT_PASS en .env"
}

$postgresUser = Read-DotEnvValue $EnvFile "POSTGRES_USER"
$postgresPassword = Read-DotEnvValue $EnvFile "POSTGRES_PASSWORD"
if ([string]::IsNullOrWhiteSpace($postgresUser)) {
    throw "Falta POSTGRES_USER en .env"
}
if ([string]::IsNullOrWhiteSpace($postgresPassword)) {
    throw "Falta POSTGRES_PASSWORD en .env"
}

foreach ($name in @("POSTGRES_HOST", "POSTGRES_PORT", "POSTGRES_DB", "POSTGRES_USER", "POSTGRES_PASSWORD")) {
    $value = Read-DotEnvValue $EnvFile $name
    if (-not [string]::IsNullOrWhiteSpace($value)) {
        Set-Item -Path "Env:$name" -Value $value
    }
}

foreach ($name in @("FLEETOPS_DEMO_DATA_ENABLED", "FLEETOPS_TELEMETRY_MAX_SPEED_KPH", "FLEETOPS_TELEMETRY_MIN_FUEL_PERCENT")) {
    $value = Read-DotEnvValue $EnvFile $name
    if (-not [string]::IsNullOrWhiteSpace($value)) {
        Set-Item -Path "Env:$name" -Value $value
    }
}

$env:SPRING_RABBITMQ_ADDRESSES = "localhost:5672,localhost:5673"
$env:SPRING_RABBITMQ_USERNAME = $rabbitUser
$env:SPRING_RABBITMQ_PASSWORD = $rabbitPass
$env:POSTGRES_USER = $postgresUser
$env:POSTGRES_PASSWORD = $postgresPassword
$env:SPRING_PROFILES_ACTIVE = "local"

Write-Host "Iniciando servicio-flota con RabbitMQ local publicado en localhost:5672,localhost:5673"

Push-Location $ServiceDir
try {
    & $MavenWrapper spring-boot:run
} finally {
    Pop-Location
}
