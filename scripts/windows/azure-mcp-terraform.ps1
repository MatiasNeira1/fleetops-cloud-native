param(
    [ValidateSet("Plan", "Apply")]
    [string]$Action = "Plan",
    [Parameter(Mandatory = $true)]
    [string]$McpImage,
    [switch]$AutoApprove,
    [string]$TerraformPath = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$envFile = Join-Path $repoRoot "backend\servicios\fleetops-mcp\.env"
$terraformDir = Join-Path $repoRoot "infra\terraform\fleetops-mcp"

if (-not (Test-Path -LiteralPath $envFile)) { throw "Falta el .env ignorado de fleetops-mcp." }
$values = @{}
foreach ($line in Get-Content -LiteralPath $envFile) {
    if ($line -match '^\s*([A-Z][A-Z0-9_]*)\s*=\s*(.*)$') {
        $values[$matches[1]] = $matches[2].Trim().Trim('"').Trim("'")
    }
}
if (-not $values.MCP_CLIENT_TOKEN -or $values.MCP_CLIENT_TOKEN.Length -lt 32) {
    throw "MCP_CLIENT_TOKEN debe tener al menos 32 caracteres."
}
if (-not $values.CONTROL_PLANE_API_KEY) { throw "Falta CONTROL_PLANE_API_KEY en el .env ignorado." }
if ($values.CONTROL_PLANE_TUNNEL_ID -ne 'tunnel_6af0a5b4c3e88191a7fe55129b59b8c5') {
    throw "El tunnel_id del .env no coincide con el configurado en Terraform."
}
if ($McpImage -notmatch '^acrmunicipalidaddoc\.azurecr\.io/fleetops/fleetops-mcp@sha256:[0-9a-f]{64}$') {
    throw "McpImage debe ser la imagen del ACR FleetOps fijada por digest SHA-256."
}
if (-not $TerraformPath) {
    $command = Get-Command terraform -ErrorAction SilentlyContinue
    if (-not $command) { throw "Instala Terraform 1.17.1 o indica -TerraformPath." }
    $TerraformPath = $command.Source
}
if (-not (Test-Path -LiteralPath $TerraformPath -PathType Leaf)) { throw "TerraformPath no existe." }

Push-Location $terraformDir
try {
    $env:TF_VAR_mcp_client_token = $values.MCP_CLIENT_TOKEN
    $env:TF_VAR_control_plane_api_key = $values.CONTROL_PLANE_API_KEY
    $env:TF_VAR_mcp_image = $McpImage

    & $TerraformPath init -input=false
    if ($LASTEXITCODE -ne 0) { throw "terraform init falló." }
    & $TerraformPath plan -input=false
    if ($LASTEXITCODE -ne 0) { throw "terraform plan falló." }
    if ($Action -eq "Apply") {
        if ($AutoApprove) {
            & $TerraformPath apply -input=false -auto-approve
        } else {
            & $TerraformPath apply
        }
        if ($LASTEXITCODE -ne 0) { throw "terraform apply falló." }
    }
} finally {
    Remove-Item Env:TF_VAR_mcp_client_token -ErrorAction SilentlyContinue
    Remove-Item Env:TF_VAR_control_plane_api_key -ErrorAction SilentlyContinue
    Remove-Item Env:TF_VAR_mcp_image -ErrorAction SilentlyContinue
    Pop-Location
}
