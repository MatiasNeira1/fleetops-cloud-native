$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$EnvFile = Join-Path $RepoRoot ".env"
$RabbitApiBaseUrl = "http://127.0.0.1:15672/api"

function Import-DotEnv {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Falta .env. Copia .env.example a .env y define las variables RabbitMQ locales."
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) {
            return
        }
        $parts = $line.Split("=", 2)
        if ($parts.Length -eq 2) {
            [Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process")
        }
    }
}

function Invoke-RabbitApi {
    param(
        [string] $Method,
        [string] $Path,
        [object] $Body = $null
    )

    $user = $env:RABBITMQ_DEFAULT_USER
    $pass = $env:RABBITMQ_DEFAULT_PASS
    if ([string]::IsNullOrWhiteSpace($user) -or [string]::IsNullOrWhiteSpace($pass)) {
        throw "Faltan RABBITMQ_DEFAULT_USER o RABBITMQ_DEFAULT_PASS en .env"
    }

    $pair = "$user`:$pass"
    $uri = "$RabbitApiBaseUrl$Path"
    $httpMethod = $Method.ToUpperInvariant()
    $curlArgs = @(
        "-sS",
        "-u", $pair,
        "-X", $httpMethod,
        "-w", "`n%{http_code}"
    )

    if ($Body -ne $null) {
        $bodyFile = New-TemporaryFile
        $Body | ConvertTo-Json -Depth 10 -Compress |
            Set-Content -LiteralPath $bodyFile -NoNewline -Encoding ASCII
        $curlArgs += @(
            "-H", "content-type: application/json",
            "--data-binary", "@$bodyFile"
        )
    }

    $curlArgs += $uri
    try {
        $raw = & curl.exe @curlArgs 2>$null
        if ($LASTEXITCODE -ne 0) {
            throw "No es posible conectar con RabbitMQ Management API en $RabbitApiBaseUrl. Revisa que el puerto 15672 este publicado y que rabbitmq-1 este iniciado."
        }
    } finally {
        if ($bodyFile) {
            Remove-Item -LiteralPath $bodyFile -ErrorAction SilentlyContinue
        }
    }

    $statusCode = [int] $raw[-1]
    $body = if ($raw.Count -gt 1) {
        ($raw[0..($raw.Count - 2)] -join "`n")
    } else {
        ""
    }

    if ($statusCode -eq 401 -or $statusCode -eq 403) {
        throw "RabbitMQ Management API rechazo las credenciales para $Path. Revisa RABBITMQ_DEFAULT_USER/RABBITMQ_DEFAULT_PASS en .env."
    }
    if ($statusCode -eq 404) {
        throw "RabbitMQ Management API respondio 404 para $Path. Revisa que el recurso exista y que el vhost / este codificado como %2F."
    }
    if ($statusCode -lt 200 -or $statusCode -ge 300) {
        throw "RabbitMQ Management API respondio HTTP $statusCode para $Path."
    }

    if ([string]::IsNullOrWhiteSpace($body)) {
        return $null
    }

    return $body | ConvertFrom-Json
}

function Wait-RabbitApi {
    param([int] $TimeoutSeconds = 60)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-RabbitApi "Get" "/overview" | Out-Null
            Write-Host "RabbitMQ Management API disponible en $RabbitApiBaseUrl"
            return
        }
        catch {
            $message = $_.Exception.Message
            if (
                $message -like "*credenciales*" -or
                $message -like "*404*" -or
                $message -like "*HTTP*"
            ) {
                throw
            }
            Start-Sleep -Seconds 2
        }
    }

    throw "RabbitMQ Management API no respondio en $RabbitApiBaseUrl despues de ${TimeoutSeconds}s."
}

function Publish-Message {
    param(
        [string] $Exchange,
        [string] $RoutingKey,
        [string] $Payload
    )

    $response = Invoke-RabbitApi "Post" "/exchanges/%2F/$Exchange/publish" @{
        properties = @{
            content_type = "application/json"
        }
        routing_key = $RoutingKey
        payload = ($Payload | ConvertTo-Json -Compress)
        payload_encoding = "string"
    }
    if (-not $response.routed) {
        throw "El mensaje no fue ruteado por $Exchange con routing key $RoutingKey"
    }
}

function Purge-Queue {
    param([string] $Queue)
    Invoke-RabbitApi "Delete" "/queues/%2F/$Queue/contents" | Out-Null
}

function Queue-MessageCount {
    param([string] $Queue)
    $queueInfo = Invoke-RabbitApi "Get" "/queues/%2F/$Queue"
    return [int] $queueInfo.messages
}

function Get-OneMessage {
    param(
        [string] $Queue,
        [string] $AckMode
    )

    $messages = Invoke-RabbitApi "Post" "/queues/%2F/$Queue/get" @{
        count = 1
        ackmode = $AckMode
        encoding = "auto"
        truncate = 50000
    }
    if ($messages.Count -lt 1) {
        throw "No se pudo obtener mensaje desde $Queue"
    }
}

function Wait-QueueMessages {
    param(
        [string] $Queue,
        [int] $Expected,
        [int] $TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $count = Queue-MessageCount $Queue
        if ($count -ge $Expected) {
            Write-Host "$Queue tiene $count mensaje(s)"
            return
        }
        Start-Sleep -Seconds 1
    }

    throw "$Queue no alcanzo $Expected mensaje(s)"
}

function Run-DlqCase {
    param(
        [string] $Name,
        [string] $Exchange,
        [string] $RoutingKey,
        [string] $MainQueue,
        [string] $Dlq
    )

    Write-Host "Caso DLQ: $Name"
    Purge-Queue $MainQueue
    $initialDlqMessages = Queue-MessageCount $Dlq

    Publish-Message $Exchange $RoutingKey "fleetops demo valido $Name"
    Wait-QueueMessages $MainQueue 1
    Get-OneMessage $MainQueue "ack_requeue_false"

    Publish-Message $Exchange $RoutingKey "fleetops demo rechazo controlado $Name"
    Wait-QueueMessages $MainQueue 1
    Get-OneMessage $MainQueue "reject_requeue_false"
    Wait-QueueMessages $Dlq ($initialDlqMessages + 1)
}

Import-DotEnv $EnvFile
Wait-RabbitApi

Run-DlqCase "general" "fleetops.logs.direct" "INFO" "fleetops.logs.all.queue" "fleetops.logs.all.dlq"
Run-DlqCase "errores" "fleetops.events.topic" "fleetops.vehicle.error" "fleetops.logs.errors.queue" "fleetops.logs.errors.dlq"
Run-DlqCase "seguridad" "fleetops.events.topic" "fleetops.security.audit" "fleetops.logs.security.queue" "fleetops.logs.security.dlq"

Purge-Queue "fleetops.logs.all.queue"
Purge-Queue "fleetops.logs.errors.queue"
Purge-Queue "fleetops.logs.security.queue"

Write-Host "Demo DLQ OK: las tres DLQ recibieron mensajes"
