$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$ComposeFile = Join-Path $RepoRoot "platform\local\docker-compose.yml"
$EnvFile = Join-Path $RepoRoot ".env"
$RabbitApiBaseUrl = "http://127.0.0.1:15672/api"

function Import-DotEnv {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Falta .env. Copia .env.example a .env y define RABBITMQ_DEFAULT_USER, RABBITMQ_DEFAULT_PASS y RABBITMQ_ERLANG_COOKIE."
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

function Compose {
    param([string[]] $Arguments)

    $dockerArgs = @("compose", "--env-file", $EnvFile, "-f", $ComposeFile) + $Arguments
    & docker @dockerArgs
}

function Wait-Healthy {
    param(
        [string] $ContainerName,
        [int] $TimeoutSeconds = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $status = (& docker inspect --format "{{.State.Health.Status}}" $ContainerName 2>$null)
        if ($status -eq "healthy") {
            Write-Host "$ContainerName healthy"
            return
        }
        Start-Sleep -Seconds 3
    }

    throw "$ContainerName no quedo healthy dentro de ${TimeoutSeconds}s"
}

function Get-ClusterStatus {
    param([string] $Service)

    Compose @("exec", "-T", $Service, "rabbitmqctl", "cluster_status")
}

function Test-ClusterStatusHasBothNodes {
    param([string[]] $Status)

    $joined = $Status -join "`n"
    return (
        $joined -match "rabbit@rabbitmq-1" -and
        $joined -match "rabbit@rabbitmq-2"
    )
}

function Repair-ClusterIfNeeded {
    $status1 = Get-ClusterStatus "rabbitmq-1"
    $status2 = Get-ClusterStatus "rabbitmq-2"

    if (
        (Test-ClusterStatusHasBothNodes $status1) -and
        (Test-ClusterStatusHasBothNodes $status2)
    ) {
        return
    }

    Write-Host "rabbitmq-2 no esta unido a rabbitmq-1. Ejecutando union idempotente del cluster..."

    Compose @("exec", "-T", "rabbitmq-2", "rabbitmqctl", "stop_app") | Out-Null
    Compose @("exec", "-T", "rabbitmq-2", "rabbitmqctl", "reset") | Out-Null
    Compose @("exec", "-T", "rabbitmq-2", "rabbitmqctl", "join_cluster", "rabbit@rabbitmq-1") | Out-Null
    Compose @("exec", "-T", "rabbitmq-2", "rabbitmqctl", "start_app") | Out-Null
}

function Invoke-RabbitApi {
    param([string] $Path)

    $user = $env:RABBITMQ_DEFAULT_USER
    $pass = $env:RABBITMQ_DEFAULT_PASS
    if ([string]::IsNullOrWhiteSpace($user) -or [string]::IsNullOrWhiteSpace($pass)) {
        throw "Faltan RABBITMQ_DEFAULT_USER o RABBITMQ_DEFAULT_PASS en .env"
    }

    $pair = "$user`:$pass"
    $uri = "$RabbitApiBaseUrl$Path"

    $raw = & curl.exe -sS -u $pair -X GET -w "`n%{http_code}" $uri 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "No es posible conectar con RabbitMQ Management API en $RabbitApiBaseUrl. Revisa que el puerto 15672 este publicado y que rabbitmq-1 este iniciado."
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
            Invoke-RabbitApi "/overview" | Out-Null
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

function Assert-Exchange {
    param(
        [object[]] $Exchanges,
        [string] $Name,
        [string] $Type
    )

    $exchange = $Exchanges | Where-Object { $_.name -eq $Name -and $_.vhost -eq "/" } | Select-Object -First 1
    if (-not $exchange) {
        throw "No existe exchange $Name"
    }
    if ($exchange.type -ne $Type -or -not $exchange.durable) {
        throw "Exchange $Name no es $Type durable"
    }
    Write-Host "Exchange OK: $Name ($Type, durable)"
}

function Assert-Queue {
    param(
        [object[]] $Queues,
        [string] $Name,
        [string] $DeadLetterExchange,
        [string] $DeadLetterRoutingKey
    )

    $queue = $Queues | Where-Object { $_.name -eq $Name -and $_.vhost -eq "/" } | Select-Object -First 1
    if (-not $queue) {
        throw "No existe cola $Name"
    }
    if (-not $queue.durable) {
        throw "La cola $Name no es durable"
    }
    if ($DeadLetterExchange) {
        if ($queue.arguments.'x-dead-letter-exchange' -ne $DeadLetterExchange) {
            throw "La cola $Name no tiene x-dead-letter-exchange=$DeadLetterExchange"
        }
        if ($queue.arguments.'x-dead-letter-routing-key' -ne $DeadLetterRoutingKey) {
            throw "La cola $Name no tiene x-dead-letter-routing-key=$DeadLetterRoutingKey"
        }
    }
    Write-Host "Cola OK: $Name"
}

function Assert-Binding {
    param(
        [object[]] $Bindings,
        [string] $Source,
        [string] $Destination,
        [string] $RoutingKey
    )

    $binding = $Bindings | Where-Object {
        $_.source -eq $Source -and
        $_.destination -eq $Destination -and
        $_.routing_key -eq $RoutingKey
    } | Select-Object -First 1
    if (-not $binding) {
        throw "No existe binding $Source -> $Destination con routing key $RoutingKey"
    }
    Write-Host "Binding OK: $Source -> $Destination ($RoutingKey)"
}

Import-DotEnv $EnvFile

Push-Location $RepoRoot
try {
    Wait-Healthy "fleetops-rabbitmq-1"
    Wait-Healthy "fleetops-rabbitmq-2"
    Wait-RabbitApi

    Repair-ClusterIfNeeded

    $status1 = Get-ClusterStatus "rabbitmq-1"
    $status2 = Get-ClusterStatus "rabbitmq-2"

    foreach ($status in @($status1, $status2)) {
        if (-not (Test-ClusterStatusHasBothNodes $status)) {
            throw "cluster_status no muestra ambos nodos"
        }
    }

    Write-Host "Cluster OK: rabbit@rabbitmq-1 y rabbit@rabbitmq-2 visibles desde ambos nodos"

    $exchanges = Invoke-RabbitApi "/exchanges/%2F"
    $queues = Invoke-RabbitApi "/queues/%2F"
    $bindings = Invoke-RabbitApi "/bindings/%2F"

    Assert-Exchange $exchanges "fleetops.logs.direct" "direct"
    Assert-Exchange $exchanges "fleetops.events.topic" "topic"
    Assert-Exchange $exchanges "fleetops.dlx.direct" "direct"

    Assert-Queue $queues "fleetops.logs.all.queue" "fleetops.dlx.direct" "fleetops.logs.all.dead"
    Assert-Queue $queues "fleetops.logs.errors.queue" "fleetops.dlx.direct" "fleetops.logs.errors.dead"
    Assert-Queue $queues "fleetops.logs.security.queue" "fleetops.dlx.direct" "fleetops.logs.security.dead"
    Assert-Queue $queues "fleetops.logs.all.dlq" $null $null
    Assert-Queue $queues "fleetops.logs.errors.dlq" $null $null
    Assert-Queue $queues "fleetops.logs.security.dlq" $null $null

    Assert-Binding $bindings "fleetops.logs.direct" "fleetops.logs.all.queue" "INFO"
    Assert-Binding $bindings "fleetops.logs.direct" "fleetops.logs.all.queue" "WARNING"
    Assert-Binding $bindings "fleetops.logs.direct" "fleetops.logs.all.queue" "ERROR"
    Assert-Binding $bindings "fleetops.logs.direct" "fleetops.logs.errors.queue" "ERROR"
    Assert-Binding $bindings "fleetops.logs.direct" "fleetops.logs.security.queue" "SECURITY"
    Assert-Binding $bindings "fleetops.events.topic" "fleetops.logs.all.queue" "fleetops.#"
    Assert-Binding $bindings "fleetops.events.topic" "fleetops.logs.errors.queue" "fleetops.*.error"
    Assert-Binding $bindings "fleetops.events.topic" "fleetops.logs.security.queue" "fleetops.security.#"
    Assert-Binding $bindings "fleetops.dlx.direct" "fleetops.logs.all.dlq" "fleetops.logs.all.dead"
    Assert-Binding $bindings "fleetops.dlx.direct" "fleetops.logs.errors.dlq" "fleetops.logs.errors.dead"
    Assert-Binding $bindings "fleetops.dlx.direct" "fleetops.logs.security.dlq" "fleetops.logs.security.dead"

    Write-Host "RabbitMQ cluster y topologia OK"
} finally {
    Pop-Location
}
