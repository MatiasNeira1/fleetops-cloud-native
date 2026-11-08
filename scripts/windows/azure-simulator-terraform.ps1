param(
    [ValidateSet('Plan', 'Apply')][string]$Action = 'Plan',
    [Parameter(Mandatory = $true)][string]$SimulatorImage,
    [Parameter(Mandatory = $true)][string]$FlotaImage,
    [Parameter(Mandatory = $true)][string]$AlertaImage,
    [switch]$SimulatorEnabled,
    [string]$TerraformPath = ''
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '../..')
$terraformDir = Join-Path $repoRoot 'infra/terraform/fleetops-simulator'
$envFile = Join-Path $repoRoot '.env'
$expected = @{
    SimulatorImage = '^acrmunicipalidaddoc\.azurecr\.io/fleetops/fleetops-simulator@sha256:[0-9a-f]{64}$'
    FlotaImage = '^acrmunicipalidaddoc\.azurecr\.io/fleetops/servicio-flota@sha256:[0-9a-f]{64}$'
    AlertaImage = '^acrmunicipalidaddoc\.azurecr\.io/fleetops/ms-alerta@sha256:[0-9a-f]{64}$'
}
foreach ($name in $expected.Keys) {
    if ((Get-Variable -Name $name -ValueOnly) -notmatch $expected[$name]) {
        throw "$name debe ser una imagen del ACR FleetOps fijada por digest SHA-256."
    }
}
$settings = @{}
if (Test-Path -LiteralPath $envFile) {
    foreach ($line in Get-Content -LiteralPath $envFile) {
        if ($line -match '^\s*([A-Z][A-Z0-9_]*)\s*=\s*(.*)$') {
            $settings[$matches[1]] = $matches[2].Trim().Trim('"').Trim("'")
        }
    }
}
foreach ($name in @('SIMULATOR_ADMIN_TOKEN', 'FLEETOPS_SIMULATOR_SERVICE_TOKEN')) {
    $fromEnvironment = [Environment]::GetEnvironmentVariable($name)
    if ($fromEnvironment) { $settings[$name] = $fromEnvironment }
}
if (-not $settings.SIMULATOR_ADMIN_TOKEN -or $settings.SIMULATOR_ADMIN_TOKEN.Length -lt 32) {
    throw 'Falta SIMULATOR_ADMIN_TOKEN de al menos 32 caracteres en el entorno o .env.'
}
if (-not $settings.FLEETOPS_SIMULATOR_SERVICE_TOKEN -or $settings.FLEETOPS_SIMULATOR_SERVICE_TOKEN.Length -lt 32) {
    throw 'Falta FLEETOPS_SIMULATOR_SERVICE_TOKEN de al menos 32 caracteres en el entorno o .env.'
}
if (-not $TerraformPath) {
    $tool = Get-Command terraform -ErrorAction SilentlyContinue
    if (-not $tool) { throw 'Instala Terraform 1.17.1 o indica -TerraformPath.' }
    $TerraformPath = $tool.Source
}

$subscription = 'cb7d7424-2923-42e4-9fa3-b2659b7890a7'
$resourceId = "/subscriptions/$subscription/resourceGroups/rg-municipalidad/providers/Microsoft.App/containerApps/ca-fleetops-servicio-flota"
$apiVersion = '2025-07-01'
$accessToken = (az account get-access-token --resource https://management.azure.com/ --query accessToken -o tsv)
if ($LASTEXITCODE -ne 0 -or -not $accessToken) { throw 'No se pudo obtener acceso ARM con Azure CLI.' }
$headers = @{ Authorization = "Bearer $accessToken" }
$flota = Invoke-RestMethod -Uri "https://management.azure.com$resourceId`?api-version=$apiVersion" -Headers $headers
$rabbitAddress = ($flota.properties.template.containers[0].env | Where-Object name -eq 'SPRING_RABBITMQ_ADDRESSES').value
$rabbitUser = ($flota.properties.template.containers[0].env | Where-Object name -eq 'SPRING_RABBITMQ_USERNAME').value
if (-not $rabbitAddress -or -not $rabbitUser) { throw 'Falta la configuración RabbitMQ existente de flota.' }
$existing = Invoke-RestMethod -Method Post -Uri "https://management.azure.com$resourceId/listSecrets?api-version=$apiVersion" `
    -Headers $headers -ContentType 'application/json' -Body '{}'
$secrets = @($existing.value | ForEach-Object {
    if (-not $_.name -or -not $_.value) { throw 'Hay un secreto existente que no puede preservarse; se cancela el despliegue.' }
    @{ name = $_.name; value = $_.value }
})
$rabbitPassword = ($secrets | Where-Object name -eq 'rabbitmq-password' | Select-Object -First 1).value
if (-not $rabbitPassword) { throw 'No se encontró el secreto RabbitMQ existente de flota.' }

try {
    $env:TF_VAR_simulator_image = $SimulatorImage
    $env:TF_VAR_simulator_enabled = if ($SimulatorEnabled) { 'true' } else { 'false' }
    $env:TF_VAR_simulator_admin_token = $settings.SIMULATOR_ADMIN_TOKEN
    $env:TF_VAR_simulator_service_token = $settings.FLEETOPS_SIMULATOR_SERVICE_TOKEN
    $env:TF_VAR_rabbitmq_addresses = $rabbitAddress
    $env:TF_VAR_rabbitmq_username = $rabbitUser
    $env:TF_VAR_rabbitmq_password = $rabbitPassword

    Push-Location $terraformDir
    try {
        & $TerraformPath init -input=false -reconfigure
        if ($LASTEXITCODE -ne 0) { throw 'terraform init falló.' }
        & $TerraformPath validate
        if ($LASTEXITCODE -ne 0) { throw 'terraform validate falló.' }
        & $TerraformPath plan -input=false
        if ($LASTEXITCODE -ne 0) { throw 'terraform plan falló.' }
        if ($Action -eq 'Apply') {
            $serviceSecret = $secrets | Where-Object name -eq 'simulator-service-token' | Select-Object -First 1
            if (-not $serviceSecret -or $serviceSecret.value -ne $settings.FLEETOPS_SIMULATOR_SERVICE_TOKEN) {
                $keep = @($secrets | Where-Object name -ne 'simulator-service-token')
                $keep += @{ name = 'simulator-service-token'; value = $settings.FLEETOPS_SIMULATOR_SERVICE_TOKEN }
                $payload = @{ properties = @{ configuration = @{ secrets = $keep } } } | ConvertTo-Json -Depth 12 -Compress
                Invoke-RestMethod -Method Patch -Uri "https://management.azure.com$resourceId`?api-version=$apiVersion" `
                    -Headers $headers -ContentType 'application/json' -Body $payload | Out-Null
            }
            $requiredSecretNames = @('postgres-password', 'rabbitmq-password', 'simulator-service-token')
            $preserved = $false
            for ($attempt = 0; $attempt -lt 40; $attempt++) {
                $after = Invoke-RestMethod -Uri "https://management.azure.com$resourceId`?api-version=$apiVersion" -Headers $headers
                $secretNames = @($after.properties.configuration.secrets | ForEach-Object { $_.name })
                if ($after.properties.provisioningState -eq 'Failed') { throw 'Falló la operación ARM de flota.' }
                if ($after.properties.provisioningState -eq 'Succeeded' -and
                    @($requiredSecretNames | Where-Object { $_ -notin $secretNames }).Count -eq 0) {
                    $preserved = $true
                    break
                }
                Start-Sleep -Seconds 5
            }
            if (-not $preserved) { throw 'La actualización de secretos de flota no conservó todas las referencias esperadas.' }
            $serviceEnv = $after.properties.template.containers[0].env |
                Where-Object name -eq 'FLEETOPS_SIMULATOR_SERVICE_TOKEN' | Select-Object -First 1
            if ($after.properties.template.containers[0].image -ne $FlotaImage -or
                -not $serviceEnv -or $serviceEnv.secretRef -ne 'simulator-service-token') {
                az containerapp update -g rg-municipalidad -n ca-fleetops-servicio-flota --image $FlotaImage `
                    --set-env-vars FLEETOPS_SIMULATOR_SERVICE_TOKEN=secretref:simulator-service-token --output none
                if ($LASTEXITCODE -ne 0) { throw 'No se pudo actualizar flota.' }
            }
            $currentAlertaImage = az containerapp show -g rg-municipalidad -n ca-fleetops-ms-alerta `
                --query 'properties.template.containers[0].image' -o tsv
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar ms-alerta.' }
            if ($currentAlertaImage -ne $AlertaImage) {
                az containerapp update -g rg-municipalidad -n ca-fleetops-ms-alerta --image $AlertaImage --output none
                if ($LASTEXITCODE -ne 0) { throw 'No se pudo actualizar ms-alerta.' }
            }
            & $TerraformPath apply -input=false -auto-approve
            if ($LASTEXITCODE -ne 0) { throw 'terraform apply falló.' }
        }
    } finally { Pop-Location }
} finally {
    foreach ($name in @('TF_VAR_simulator_image','TF_VAR_simulator_enabled','TF_VAR_simulator_admin_token',
                       'TF_VAR_simulator_service_token','TF_VAR_rabbitmq_addresses','TF_VAR_rabbitmq_username',
                       'TF_VAR_rabbitmq_password')) {
        Remove-Item "Env:$name" -ErrorAction SilentlyContinue
    }
    $accessToken = $null
    $rabbitPassword = $null
    $secrets = $null
    $payload = $null
}
