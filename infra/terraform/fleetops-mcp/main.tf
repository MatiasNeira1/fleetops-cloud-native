terraform {
  required_version = ">= 1.17.1"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "= 5.13.0"
    }
  }

  backend "azurerm" {
    resource_group_name  = "rg-fleetops-dev"
    storage_account_name = "stfleetopsweb"
    container_name       = "tfstate-mcp"
    key                  = "fleetops-mcp/dev.tfstate"
    use_azuread_auth     = true
    use_cli              = true
  }
}

provider "azurerm" {
  subscription_id                 = var.subscription_id
  resource_provider_registrations = "none"
  features {}
}

data "azurerm_resource_group" "fleetops" {
  name = "rg-municipalidad"
}

data "azurerm_container_app_environment" "fleetops" {
  name                = "cae-fleetops-dev"
  resource_group_name = data.azurerm_resource_group.fleetops.name
}

data "azurerm_container_registry" "fleetops" {
  name                = "acrmunicipalidaddoc"
  resource_group_name = data.azurerm_resource_group.fleetops.name
}

data "azurerm_container_app" "flota" {
  name                = "ca-fleetops-servicio-flota"
  resource_group_name = data.azurerm_resource_group.fleetops.name
}

data "azurerm_container_app" "alerta" {
  name                = "ca-fleetops-ms-alerta"
  resource_group_name = data.azurerm_resource_group.fleetops.name
}

resource "azurerm_user_assigned_identity" "mcp" {
  name                = "id-fleetops-mcp"
  location            = data.azurerm_resource_group.fleetops.location
  resource_group_name = data.azurerm_resource_group.fleetops.name
  tags                = local.tags
}

resource "azurerm_role_assignment" "mcp_acr_pull" {
  scope                            = data.azurerm_container_registry.fleetops.id
  role_definition_name             = "AcrPull"
  principal_id                     = azurerm_user_assigned_identity.mcp.principal_id
  principal_type                   = "ServicePrincipal"
  skip_service_principal_aad_check = true
}

locals {
  tunnel_id = "tunnel_6af0a5b4c3e88191a7fe55129b59b8c5"
  tags = {
    project     = "fleetops"
    component   = "mcp"
    environment = "dev"
  }
}

resource "azurerm_container_app" "mcp" {
  name                         = "ca-fleetops-mcp"
  resource_group_name          = data.azurerm_resource_group.fleetops.name
  container_app_environment_id = data.azurerm_container_app_environment.fleetops.id
  workload_profile_name        = "Consumption"
  revision_mode                = "Single"
  tags                         = local.tags

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.mcp.id]
  }

  registry {
    server   = data.azurerm_container_registry.fleetops.login_server
    identity = azurerm_user_assigned_identity.mcp.id
  }

  secret {
    name  = "mcp-client-token"
    value = var.mcp_client_token
  }

  secret {
    name  = "control-plane-api-key"
    value = var.control_plane_api_key
  }

  template {
    min_replicas = 1
    max_replicas = 1

    container {
      name   = "fleetops-mcp"
      image  = var.mcp_image
      cpu    = 0.5
      memory = "1Gi"

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }
      env {
        name  = "MCP_PORT"
        value = "8088"
      }
      env {
        name  = "MCP_BIND_ADDRESS"
        value = "127.0.0.1"
      }
      env {
        name        = "MCP_CLIENT_TOKEN"
        secret_name = "mcp-client-token"
      }
      env {
        name  = "FLEETOPS_FLOTA_API_URL"
        value = "http://${data.azurerm_container_app.flota.name}"
      }
      env {
        name  = "FLEETOPS_ALERTA_API_URL"
        value = "http://${data.azurerm_container_app.alerta.name}"
      }
    }

    container {
      name    = "tunnel-client"
      image   = "ghcr.io/openai/tunnel-client:v0.0.20@sha256:333005e6f5df2760f1b9f06fe873f5fc629554218f68c91d9cfbfe467d0fb42b"
      cpu     = 0.25
      memory  = "0.5Gi"
      command = ["/bin/sh"]
      args    = ["-ec", "export MCP_TUNNEL_LOCAL_AUTHORIZATION=\"Bearer $${MCP_CLIENT_TOKEN}\"; exec /usr/bin/tunnel-client run"]

      env {
        name        = "MCP_CLIENT_TOKEN"
        secret_name = "mcp-client-token"
      }
      env {
        name        = "CONTROL_PLANE_API_KEY"
        secret_name = "control-plane-api-key"
      }
      env {
        name  = "CONTROL_PLANE_TUNNEL_ID"
        value = local.tunnel_id
      }
      env {
        name  = "MCP_SERVER_URL"
        value = "http://127.0.0.1:8088/mcp"
      }
      env {
        name  = "MCP_EXTRA_HEADERS"
        value = "Authorization: env:MCP_TUNNEL_LOCAL_AUTHORIZATION"
      }
      env {
        name  = "MCP_DISCOVERY_EXTRA_HEADERS"
        value = "Authorization: env:MCP_TUNNEL_LOCAL_AUTHORIZATION"
      }
      env {
        name  = "MCP_STARTUP_WAIT_TIMEOUT"
        value = "60s"
      }
      env {
        name  = "HEALTH_LISTEN_ADDR"
        value = ":8090"
      }
      env {
        name  = "LOG_LEVEL"
        value = "info"
      }
      env {
        name  = "LOG_FORMAT"
        value = "json"
      }

      startup_probe {
        transport               = "HTTP"
        port                    = 8090
        path                    = "/healthz"
        interval_seconds        = 2
        failure_count_threshold = 30
      }
      liveness_probe {
        transport        = "HTTP"
        port             = 8090
        path             = "/healthz"
        interval_seconds = 10
      }
      readiness_probe {
        transport               = "HTTP"
        port                    = 8090
        path                    = "/readyz"
        interval_seconds        = 5
        failure_count_threshold = 3
        success_count_threshold = 1
      }
    }
  }

  depends_on = [azurerm_role_assignment.mcp_acr_pull]
}
