$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$ComposeFile = Join-Path $RepoRoot "platform\local\docker-compose.yml"
$EnvFile = Join-Path $RepoRoot ".env"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker no esta disponible en PATH. Instala Docker Desktop o inicia una terminal con Docker disponible."
}

$DockerArgs = @("compose")
if (Test-Path -LiteralPath $EnvFile) {
    $DockerArgs += @("--env-file", $EnvFile)
}
$DockerArgs += @("-f", $ComposeFile, "up", "-d")

Push-Location $RepoRoot
try {
    & docker @DockerArgs
} finally {
    Pop-Location
}
