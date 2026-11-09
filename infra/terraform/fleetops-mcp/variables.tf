variable "subscription_id" {
  type        = string
  description = "Suscripción Azure for Students que contiene rg-municipalidad."
  default     = "cb7d7424-2923-42e4-9fa3-b2659b7890a7"
}

variable "mcp_image" {
  type        = string
  description = "Imagen MCP publicada en el ACR FleetOps, fijada por digest."

  validation {
    condition     = can(regex("^acrmunicipalidaddoc\\.azurecr\\.io/fleetops/fleetops-mcp@sha256:[0-9a-f]{64}$", var.mcp_image))
    error_message = "mcp_image debe ser la imagen fleetops/fleetops-mcp del ACR existente, fijada por SHA-256."
  }
}

variable "mcp_client_token" {
  type        = string
  sensitive   = true
  description = "Token local de /mcp, provisto mediante TF_VAR_mcp_client_token; queda en el estado Terraform cifrado."

  validation {
    condition     = length(var.mcp_client_token) >= 32
    error_message = "mcp_client_token debe tener al menos 32 caracteres."
  }
}

variable "control_plane_api_key" {
  type        = string
  sensitive   = true
  description = "Runtime API key del túnel, provista mediante TF_VAR_control_plane_api_key; queda en el estado Terraform cifrado."

  validation {
    condition     = length(var.control_plane_api_key) > 0
    error_message = "control_plane_api_key es obligatoria."
  }
}
