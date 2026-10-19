param(
    [string] $BaseUrl = "http://localhost:8080",
    [int] $TimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"

function Invoke-FleetOpsRequest {
    param(
        [string] $Name,
        [string] $Method,
        [string] $Path,
        [object] $Body,
        [int[]] $ExpectedStatus
    )

    $uri = "$BaseUrl$Path"
    $headers = @{}
    $bodyJson = $null
    if ($null -ne $Body) {
        $headers["Content-Type"] = "application/json"
        $bodyJson = $Body | ConvertTo-Json -Depth 10
    }

    try {
        $response = Invoke-WebRequest -Method $Method -Uri $uri -Headers $headers -Body $bodyJson -UseBasicParsing
        $statusCode = [int] $response.StatusCode
        $content = $response.Content
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int] $_.Exception.Response.StatusCode
            $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            $content = $reader.ReadToEnd()
        } else {
            throw
        }
    }

    if ($ExpectedStatus -notcontains $statusCode) {
        throw "$Name fallo: HTTP $statusCode en $Method $Path. Respuesta: $content"
    }

    Write-Host ("OK {0} -> HTTP {1}" -f $Name, $statusCode)
    if ([string]::IsNullOrWhiteSpace($content)) {
        return $null
    }
    return $content | ConvertFrom-Json
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
do {
    try {
        Invoke-FleetOpsRequest -Name "health" -Method "GET" -Path "/actuator/health" -ExpectedStatus @(200) | Out-Null
        break
    } catch {
        if ((Get-Date) -ge $deadline) {
            throw "La API no respondio en $BaseUrl despues de $TimeoutSeconds segundos. Inicia el backend con ./scripts/windows/local-backend-run.ps1"
        }
        Start-Sleep -Seconds 2
    }
} while ($true)

$trips = Invoke-FleetOpsRequest -Name "listar viajes" -Method "GET" -Path "/api/v1/trips" -ExpectedStatus @(200)
if (-not $trips -or $trips.Count -eq 0) {
    throw "No hay viajes demo. Define FLEETOPS_DEMO_DATA_ENABLED=true en .env y reinicia el backend con una base vacia."
}

Invoke-FleetOpsRequest -Name "filtrar viajes IN_TRANSIT" -Method "GET" -Path "/api/v1/trips?status=IN_TRANSIT" -ExpectedStatus @(200) | Out-Null
Invoke-FleetOpsRequest -Name "detalle trip-001" -Method "GET" -Path "/api/v1/trips/trip-001" -ExpectedStatus @(200) | Out-Null
Invoke-FleetOpsRequest -Name "alertas trip-003" -Method "GET" -Path "/api/v1/trips/trip-003/alerts" -ExpectedStatus @(200) | Out-Null
Invoke-FleetOpsRequest -Name "posicion vehicle-001" -Method "GET" -Path "/api/v1/vehicles/vehicle-001/position" -ExpectedStatus @(200) | Out-Null

$telemetry = @{
    eventId = [guid]::NewGuid().ToString()
    vehicleId = "vehicle-001"
    tripId = "trip-001"
    latitude = -33.2321
    longitude = -71.0201
    speedKph = 95.5
    fuelPercent = 10.2
    odometerKm = 128540.7
    recordedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
}
Invoke-FleetOpsRequest -Name "enviar telemetria" -Method "POST" -Path "/api/v1/telemetry" -Body $telemetry -ExpectedStatus @(202) | Out-Null

$incidentRequest = @{
    tripId = "trip-001"
    vehicleId = "vehicle-001"
    type = "MECHANICAL_FAILURE"
    title = "Smoke test incidente"
    description = "Incidente generado por api-smoke-test.ps1"
    severity = "CRITICAL"
    latitude = -33.2321
    longitude = -71.0201
}
$incident = Invoke-FleetOpsRequest -Name "crear incidente" -Method "POST" -Path "/api/v1/incidents" -Body $incidentRequest -ExpectedStatus @(201)

$statusRequest = @{
    status = "RESOLVED"
    resolutionNote = "Resuelto por smoke test"
}
Invoke-FleetOpsRequest -Name "resolver incidente" -Method "PATCH" -Path "/api/v1/incidents/$($incident.id)/status" -Body $statusRequest -ExpectedStatus @(200) | Out-Null

Write-Host ""
Write-Host "Smoke test REST completado en $BaseUrl"
