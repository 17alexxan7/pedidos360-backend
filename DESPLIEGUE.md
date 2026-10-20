# Despliegue en la nube — Pedidos360

Arquitectura de despliegue, configuración y guía de reinicio del sistema Pedidos360 (DSY1107).
Este documento no contiene contraseñas, tokens ni cadenas de conexión: esos datos viven en variables de entorno fuera del repositorio.

## Arquitectura

```
Navegador ──► Azure Static Web Apps (Angular, HTTPS)
    │                 │ login OAuth 2.0 / OIDC
    │                 ▼
    │          Microsoft Entra ID (emite el JWT)
    │
    └──► AWS API Gateway (HTTP API, autorizador JWT)
                 │  ANY /api/{proxy+}
                 ▼
         EC2 t3.medium (Ubuntu 24.04, Elastic IP)
           ├─ ms-pedidos         :8080  productor + BD
           ├─ ms-rabbit-admin    :8081  administración de RabbitMQ
           ├─ ms-notificaciones  :8082  consumidor
           ├─ ms-tickets         :8083  consumidor
           ├─ ms-documentos      :8084  consumidor
           └─ RabbitMQ (Docker): rabbitmq-1 + rabbitmq-2 en clúster
                 │
                 ▼
         PostgreSQL en Neon (BD cloud, solo la usa ms-pedidos)
```

## Componentes

| Componente | Detalle |
|---|---|
| Frontend | Azure Static Web Apps (plan Free), HTTPS, despliegue automático con GitHub Actions al hacer push a `main` del repo del frontend. URL: `https://wonderful-sea-08ec42110.1.azurestaticapps.net` |
| Identidad (IDaaS) | Microsoft Entra ID: registro del frontend (SPA, PKCE) y registro de la API con el scope `access_as_user`. Redirect URIs tipo SPA: `http://localhost:4200` y la URL de Azure |
| API Manager | AWS API Gateway (HTTP API) `pedidos360-api`, URL `https://mxec3681gd.execute-api.us-east-1.amazonaws.com` |
| Autorizador | JWT de Entra ID (issuer v2.0 del tenant, audiencia = ID de la API). Sin token o con token alterado responde `401` |
| Rutas | `ANY /api/{proxy+}` con autorizador; `OPTIONS /api/{proxy+}` sin autorizador (preflight de CORS) |
| Servidor | EC2 `t3.medium` (4 GiB), Ubuntu 24.04 LTS, disco 20 GiB, región `us-east-1`, Elastic IP |
| RabbitMQ | 2 nodos en clúster con `infra/docker-compose.yml` (política `unless-stopped`) |
| Microservicios | 5 servicios `systemd` (`Restart=on-failure`, habilitados al arranque, `-Xmx320m`) |
| Base de datos | PostgreSQL en Neon (el cómputo gratuito se apaga solo; la primera conexión tarda unos segundos) |

## Seguridad

- **Security Group:** SSH (22) solo desde «Mi IP»; 8080-8081 abiertos; 5672, 15672 y 8082-8084 cerrados (uso interno).
- **JWT en dos capas:** el API Gateway lo valida primero y cada microservicio lo vuelve a validar (`oauth2ResourceServer`).
- **Credenciales fuera del repo:** `~/pedidos360.env` en la VM (`chmod 600`), cargado por `systemd` con `EnvironmentFile=`. Variables: `DB_URL`, `DB_USER`, `DB_PASS` y `RABBITMQ_HOST/PORT/USER/PASS`.
- **CORS:** se configura en el API Gateway y también en `CorsConfig.java` y `SecurityConfig.java` de `ms-pedidos`. Al agregar un nuevo origen hay que actualizar los tres sitios y redesplegar `ms-pedidos`.

## Guía de reinicio

1. AWS Academy → **Start Lab** y esperar el indicador verde; abrir la consola.
2. **EC2 → Instancias →** `pedidos360-vm` **→ Estado de la instancia → Iniciar**; esperar 2/2 comprobaciones.
3. Conectarse por SSH desde PowerShell (comillas por los espacios de la ruta):
   `ssh -i "<RUTA>\CloudEV2.pem" ubuntu@<ELASTIC_IP>`
   Si da *timeout*, la IP pública del equipo cambió: editar en el Security Group la regla SSH y volver a poner «Mi IP».
4. Verificar en la VM:
   ```bash
   docker ps --format "table {{.Names}}\t{{.Status}}"
   systemctl is-active ms-pedidos ms-rabbit-admin ms-notificaciones ms-tickets ms-documentos
   for p in 8080 8081 8082 8083 8084; do echo -n "puerto $p: "; curl -s localhost:$p/actuator/health; echo; done
   ```
   Debe haber `rabbitmq-1` y `rabbitmq-2`, 5 × `active` y los 5 puertos en `UP`. Los servicios Java pueden tardar 1 o 2 minutos.
5. Si RabbitMQ no subió: `cd ~/pedidos360-backend/infra && docker compose up -d`.
   Si un servicio falló: `sudo systemctl restart <nombre>` y `sudo journalctl -u <nombre> -n 20 --no-pager`.
6. Despertar Neon antes de usar la app (la primera consulta puede tardar ~20 s).

## Actualizar un servicio en la VM

```bash
cd ~/pedidos360-backend && git pull
cd ms-pedidos && ./mvnw -q clean package -DskipTests
sudo systemctl restart ms-pedidos
curl -s localhost:8080/actuator/health
```

Al desplegar desde cero, levantar `ms-pedidos` primero: es quien declara las colas y exchanges al abrir la primera conexión.

## Verificación rápida

- `GET /api/pedidos` por el gateway **sin token** → `401`; con token alterado → `401`; con token real → `200`.
- Clúster: `docker exec rabbitmq-1 rabbitmqctl cluster_status` (2 nodos de disco y 2 en ejecución).
- Exchanges: `docker exec rabbitmq-1 rabbitmqctl list_exchanges name type | grep pedidos` (`pedidos.direct`, `pedidos.topic`, `pedidos.dlx`).
- Colas: `docker exec rabbitmq-1 rabbitmqctl list_queues name messages consumers` (3 colas principales y 3 `.dlq`).
- Mensaje fallido: un `POST /api/pedidos` con `cantidad = 0` genera en los 3 consumidores `NACK -> DLQ (irrecuperable)` y el registro `MENSAJE MUERTO` del listener de DLQ.

## Apagado

- Detener la instancia desde **EC2 → Estado de la instancia → Detener** (nunca «Terminar»).
- La Elastic IP sigue cobrando aunque la instancia esté detenida; se libera solo cuando termine el proyecto.
- Si la IP cambia, hay que actualizar la integración del API Gateway (`ANY http://<ELASTIC_IP>:8080/api/{proxy}`).
