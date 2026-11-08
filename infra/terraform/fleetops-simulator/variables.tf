variable "subscription_id" {
  type    = string
  default = "cb7d7424-2923-42e4-9fa3-b2659b7890a7"
}

variable "simulator_image" {
  type = string
  validation {
    condition     = can(regex("^acrmunicipalidaddoc\\.azurecr\\.io/fleetops/fleetops-simulator@sha256:[0-9a-f]{64}$", var.simulator_image))
    error_message = "Usa una imagen del ACR FleetOps fijada por digest SHA-256."
  }
}

variable "simulator_enabled" {
  type    = bool
  default = false
}

variable "simulator_max_failures" {
  type    = number
  default = 5
  validation {
    condition     = var.simulator_max_failures >= 1 && var.simulator_max_failures <= 100
    error_message = "simulator_max_failures debe estar entre 1 y 100."
  }
}

variable "simulator_admin_token" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.simulator_admin_token) >= 32
    error_message = "El token administrativo debe tener al menos 32 caracteres."
  }
}

variable "simulator_service_token" {
  type      = string
  sensitive = true
  validation {
    condition     = length(var.simulator_service_token) >= 32
    error_message = "El token de servicio debe tener al menos 32 caracteres."
  }
}

variable "rabbitmq_addresses" {
  type = string
}

variable "rabbitmq_username" {
  type = string
}

variable "rabbitmq_password" {
  type      = string
  sensitive = true
}
