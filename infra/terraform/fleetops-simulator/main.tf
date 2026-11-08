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
    key                  = "fleetops-simulator/dev.tfstate"
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

resource "azurerm_user_assigned_identity" "simulator" {
  name                = "id-fleetops-simulator"
  location            = data.azurerm_resource_group.fleetops.location
  resource_group_name = data.azurerm_resource_group.fleetops.name
  tags                = local.tags
}

resource "azurerm_role_assignment" "acr_pull" {
  scope                            = data.azurerm_container_registry.fleetops.id
  role_definition_name             = "AcrPull"
  principal_id                     = azurerm_user_assigned_identity.simulator.principal_id
  principal_type                   = "ServicePrincipal"
  skip_service_principal_aad_check = true
}

locals {
  tags = {
    project     = "fleetops"
    component   = "simulator"
    environment = "dev"
  }
}

resource "azurerm_container_app" "simulator" {
  name                         = "ca-fleetops-simulator"
  resource_group_name          = data.azurerm_resource_group.fleetops.name
  container_app_environment_id = data.azurerm_container_app_environment.fleetops.id
  workload_profile_name        = "Consumption"
  revision_mode                = "Single"
  tags                         = local.tags

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.simulator.id]
  }

  registry {
    server   = data.azurerm_container_registry.fleetops.login_server
    identity = azurerm_user_assigned_identity.simulator.id
  }

  secret {
    name  = "simulator-admin-token"
    value = var.simulator_admin_token
  }
  secret {
    name  = "simulator-service-token"
    value = var.simulator_service_token
  }
  secret {
    name  = "rabbitmq-password"
    value = var.rabbitmq_password
  }

  template {
    min_replicas = 1
    max_replicas = 1

    container {
      name   = "fleetops-simulator"
      image  = var.simulator_image
      cpu    = 0.5
      memory = "1Gi"

      env {
        name  = "SIMULATOR_ENABLED"
        value = tostring(var.simulator_enabled)
      }
      env {
        name  = "SIMULATOR_BIND_ADDRESS"
        value = "0.0.0.0"
      }
      env {
        name  = "SIMULATOR_PORT"
        value = "8089"
      }
      env {
        name  = "SIMULATOR_MAX_VEHICLES"
        value = "3"
      }
      env {
        name  = "SIMULATOR_MAX_FAILURES"
        value = tostring(var.simulator_max_failures)
      }
      env {
        name        = "SIMULATOR_ADMIN_TOKEN"
        secret_name = "simulator-admin-token"
      }
      env {
        name        = "SIMULATOR_SERVICE_TOKEN"
        secret_name = "simulator-service-token"
      }
      env {
        name  = "FLEETOPS_FLOTA_API_URL"
        value = "http://${data.azurerm_container_app.flota.name}"
      }
      env {
        name  = "SPRING_RABBITMQ_ADDRESSES"
        value = var.rabbitmq_addresses
      }
      env {
        name  = "SPRING_RABBITMQ_USERNAME"
        value = var.rabbitmq_username
      }
      env {
        name        = "SPRING_RABBITMQ_PASSWORD"
        secret_name = "rabbitmq-password"
      }

      startup_probe {
        transport               = "HTTP"
        port                    = 8089
        path                    = "/actuator/health"
        interval_seconds        = 5
        failure_count_threshold = 24
      }
      liveness_probe {
        transport        = "HTTP"
        port             = 8089
        path             = "/actuator/health/liveness"
        interval_seconds = 15
      }
      readiness_probe {
        transport               = "HTTP"
        port                    = 8089
        path                    = "/actuator/health/readiness"
        interval_seconds        = 10
        failure_count_threshold = 3
      }
    }
  }

  depends_on = [azurerm_role_assignment.acr_pull]
}
