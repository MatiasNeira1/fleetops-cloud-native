$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$EnvExample = Join-Path $RepoRoot ".env.example"
$EnvFile = Join-Path $RepoRoot ".env"

if (-not (Test-Path -LiteralPath $EnvExample)) {
    Write-Error "Falta .env.example en la raiz del repositorio."
}

Write-Output "OK: .env.example existe."

if (Test-Path -LiteralPath $EnvFile) {
    Write-Output "OK: .env local existe y no debe versionarse."
} else {
    Write-Warning ".env no existe. Copia .env.example a .env si necesitas personalizar valores locales."
}
