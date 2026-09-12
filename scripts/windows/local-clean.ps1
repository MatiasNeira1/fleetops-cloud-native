$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$ComposeFile = Join-Path $RepoRoot "platform\local\docker-compose.yml"
$EnvFile = Join-Path $RepoRoot ".env"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker no esta disponible en PATH. Instala Docker Desktop o inicia una terminal con Docker disponible."
}

Write-Warning "Esto detendra el entorno local y eliminara volumenes Docker asociados al compose local."

$DockerArgs = @("compose")
if (Test-Path -LiteralPath $EnvFile) {
    $DockerArgs += @("--env-file", $EnvFile)
}
$DockerArgs += @("-f", $ComposeFile, "down", "-v", "--remove-orphans")

Push-Location $RepoRoot
try {
    & docker @DockerArgs
} finally {
    Pop-Location
}
