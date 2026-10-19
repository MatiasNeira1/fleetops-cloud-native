$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$envFile = Join-Path $repoRoot ".env"
if (-not (Test-Path -LiteralPath $envFile)) { throw "Falta .env en la raiz" }
Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line.Split("=", 2)
        if ($parts.Length -eq 2) { [Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process") }
    }
}
if (-not $env:RABBITMQ_DEFAULT_USER -or -not $env:RABBITMQ_DEFAULT_PASS) { throw "Faltan credenciales RabbitMQ en .env" }
$fleetUrl = "http://127.0.0.1:8080"
$alertsUrl = "http://127.0.0.1:8081"
$rabbitPort = if ($env:RABBITMQ_NODE1_MANAGEMENT_PORT) { $env:RABBITMQ_NODE1_MANAGEMENT_PORT } else { "15672" }
$rabbitUrl = "http://127.0.0.1:$rabbitPort/api"
$queue = if ($env:RABBITMQ_ALERTS_QUEUE) { $env:RABBITMQ_ALERTS_QUEUE } else { "fleetops.alerts.queue" }
$dlq = if ($env:RABBITMQ_ALERTS_DLQ) { $env:RABBITMQ_ALERTS_DLQ } else { "fleetops.alerts.dlq" }
$exchange = if ($env:RABBITMQ_ALERTS_EXCHANGE) { $env:RABBITMQ_ALERTS_EXCHANGE } else { "fleetops.events.topic" }
$pair = "$($env:RABBITMQ_DEFAULT_USER):$($env:RABBITMQ_DEFAULT_PASS)"
$auth = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
$headers = @{ Authorization = "Basic $auth" }

function Wait-Healthy([string] $baseUrl, [string] $path) {
    $endpoint = "$baseUrl/actuator/health"
    if ($path) { $endpoint = "$endpoint/$path" }
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try {
            Invoke-RestMethod $endpoint -TimeoutSec 3 | Out-Null
            return
        } catch { Start-Sleep -Seconds 2 }
    }
    throw "Servicio no saludable: $baseUrl"
}
function Rabbit([string] $path) {
    Invoke-RestMethod "$rabbitUrl$path" -Headers $headers -TimeoutSec 5
}
function Queue-Stat([string] $name, [string] $field) {
    $info = Rabbit "/queues/%2F/$name"
    if ($field -eq "ack") { return [int]$info.message_stats.ack }
    return [int]$info.messages
}

Wait-Healthy $fleetUrl ""
Wait-Healthy $alertsUrl "readiness"
if (@(Rabbit "/nodes").Count -lt 2) { throw "Cluster RabbitMQ incompleto" }
Rabbit "/queues/%2F/$queue" | Out-Null
Rabbit "/queues/%2F/$dlq" | Out-Null
$beforeAlerts = [int](Invoke-RestMethod "$alertsUrl/api/v1/trips/trip-001/alerts?size=1").totalElements
$beforeAck = Queue-Stat $queue "ack"
foreach ($kind in @("speed", "fuel")) {
    $eventId = [Guid]::NewGuid().ToString()
    $body = @{
        eventId = $eventId
        vehicleId = "vehicle-001"
        tripId = "trip-001"
        latitude = -33.2321
        longitude = -71.0201
        speedKph = if ($kind -eq "speed") { 105 } else { 50 }
        fuelPercent = if ($kind -eq "fuel") { 8 } else { 50 }
        odometerKm = 128540.7
        recordedAt = [DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ")
    } | ConvertTo-Json -Compress
    $accepted = Invoke-RestMethod "$fleetUrl/api/v1/telemetry" -Method Post -ContentType "application/json" -Body $body
    if ($accepted.status -ne "ACCEPTED") { throw "Telemetria $kind no aceptada" }
    $expected = $beforeAlerts + 1
    $current = $beforeAlerts
    $matched = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        $alerts = Invoke-RestMethod "$alertsUrl/api/v1/trips/trip-001/alerts?size=100"
        $current = [int]$alerts.totalElements
        if ($current -ge $expected -and @($alerts.content | Where-Object { $_.correlationId -eq $eventId }).Count -gt 0) {
            $matched = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not $matched) { throw "No aparecio alerta nueva de $kind con correlationId $eventId" }
    $beforeAlerts = $current
    Write-Host "Alerta $kind persistida; total del viaje: $current"
}
$afterAck = Queue-Stat $queue "ack"
if ($afterAck -lt ($beforeAck + 2)) { throw "No aumento ACK de $queue" }
$beforeDlq = Queue-Stat $dlq "messages"
$payload = @{ eventId = "invalid-controlled-demo"; eventType = "alert.created"; eventVersion = 999 } | ConvertTo-Json -Compress
$publish = @{ properties = @{ content_type = "application/json" }; routing_key = "fleetops.alert.speed.created"; payload = $payload; payload_encoding = "string" } | ConvertTo-Json -Depth 5 -Compress
$result = Invoke-RestMethod "$rabbitUrl/exchanges/%2F/$exchange/publish" -Method Post -Headers $headers -ContentType "application/json" -Body $publish
if (-not $result.routed) { throw "Mensaje invalido no fue ruteado" }
$afterDlq = $beforeDlq
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    $afterDlq = Queue-Stat $dlq "messages"
    if ($afterDlq -gt $beforeDlq) { break }
    Start-Sleep -Seconds 1
}
if ($afterDlq -le $beforeDlq) { throw "No aumento $dlq" }
Write-Host "ACK $beforeAck -> $afterAck; DLQ $beforeDlq -> $afterDlq"
