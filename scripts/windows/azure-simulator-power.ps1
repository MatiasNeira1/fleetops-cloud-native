param([Parameter(Mandatory = $true)][ValidateSet('Start', 'Stop')][string]$Action)

$ErrorActionPreference = 'Stop'
$subscription = 'cb7d7424-2923-42e4-9fa3-b2659b7890a7'
$resourceId = "/subscriptions/$subscription/resourceGroups/rg-municipalidad/providers/Microsoft.App/containerApps/ca-fleetops-simulator"
$token = az account get-access-token --resource https://management.azure.com/ --query accessToken -o tsv
if ($LASTEXITCODE -ne 0 -or -not $token) { throw 'No se pudo autenticar con Azure Resource Manager.' }
$headers = @{ Authorization = "Bearer $token" }
$operation = $Action.ToLowerInvariant()
Invoke-RestMethod -Method Post -Uri "https://management.azure.com$resourceId/$operation`?api-version=2025-07-01" `
    -Headers $headers -ContentType 'application/json' -Body '{}' | Out-Null
Write-Output "Operación $Action solicitada para ca-fleetops-simulator. Consulta provisioningState y runningStatus antes de continuar."
