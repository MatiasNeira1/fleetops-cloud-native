output "container_app_name" {
  value = azurerm_container_app.mcp.name
}

output "container_app_id" {
  value = azurerm_container_app.mcp.id
}

output "tunnel_id" {
  value = local.tunnel_id
}
