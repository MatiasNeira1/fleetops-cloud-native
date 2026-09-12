# Entornos de FleetOps

FleetOps define tres entornos iniciales: `LOCAL`, `DEV` y `DEMO`.

No se creará un ambiente `PROD` real en esta etapa. Una arquitectura productiva puede definirse más adelante, pero no será desplegada permanentemente mientras el proyecto esté limitado por presupuesto de Azure.

## LOCAL

Entorno ejecutado en el PC del desarrollador.

Características:

- Uso principal desde IntelliJ IDEA.
- Infraestructura local con Docker Compose.
- PostgreSQL local en contenedor.
- Configuración mediante archivo `.env` local no versionado.
- Futuro uso de emuladores cuando aporten valor.
- Costo Azure: 0.
- Orientado a desarrollo, pruebas rápidas y aprendizaje seguro.

## DEV

Entorno cloud en Azure para integración técnica.

Características esperadas:

- Azure Container Apps para servicios.
- Azure Database for PostgreSQL Flexible Server.
- Azure Event Hubs.
- Azure Service Bus.
- Azure Key Vault.
- Azure Monitor, Application Insights y Log Analytics.
- Terraform para aprovisionamiento.
- Recursos mínimos y controlados.
- Recursos que pueden crearse y destruirse para controlar costos.
- Configuración externa, no basada en secretos versionados.

## DEMO

Entorno cloud en Azure para demostraciones estables de portafolio y entrevistas.

Características esperadas:

- Versión candidata a demostración.
- Observabilidad habilitada.
- Pruebas k6 controladas.
- Datos ficticios.
- Sin datos personales reales.
- Mayor estabilidad que `DEV`.
- Configuración externa mediante servicios administrados.

## Comparación de entornos

| Aspecto | LOCAL | DEV | DEMO |
| --- | --- | --- | --- |
| Propósito | Desarrollo en PC del desarrollador | Integración cloud y validación técnica | Demostración estable para portafolio |
| Infraestructura | Docker Compose, PostgreSQL local | Azure con recursos mínimos | Azure con recursos estables para demo |
| Datos | Datos locales y descartables | Datos técnicos de prueba | Datos ficticios controlados |
| Secretos | `.env` local no versionado | Managed Identity y Key Vault | Managed Identity y Key Vault |
| Despliegue | Manual desde scripts locales | Terraform y despliegues controlados | Terraform y despliegues controlados |
| Costo | 0 en Azure | Bajo, recursos apagables o destruibles | Controlado, orientado a disponibilidad de demo |
| Persistencia | Volúmenes Docker locales | Persistencia limitada según necesidad | Persistencia suficiente para demos |
