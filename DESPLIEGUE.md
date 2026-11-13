# Despliegue en la nube — Pedidos360

Arquitectura de despliegue, configuración y guía de reinicio del sistema Pedidos360 (DSY1107): mensajería con RabbitMQ (EP3) y streaming de eventos con Kafka (EP5).
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
         EC2 t3.large (Ubuntu 24.04, 8 GiB, Elastic IP)
           ├─ ms-pedidos         :8080  productor RabbitMQ y Kafka + BD
           ├─ ms-rabbit-admin    :8081  administración de RabbitMQ
           ├─ ms-notificaciones  :8082  consumidor RabbitMQ
           ├─ ms-tickets         :8083  consumidor RabbitMQ
           ├─ ms-documentos      :8084  consumidor RabbitMQ
           ├─ ms-analitica       :8085  consumidor y productor Kafka
           ├─ ms-kafka-admin     :8086  administración de Kafka
           ├─ RabbitMQ (Docker): rabbitmq-1 + rabbitmq-2 en clúster
           ├─ Kafka (Docker): 3 Zookeeper + 3 brokers (kafka-1, kafka-2, kafka-3)
           └─ Kafka UI (Docker): :8090
                 │
                 ▼
         PostgreSQL en Neon (BD cloud, solo la usa ms-pedidos)
```

Flujo de un pedido: `ms-pedidos` guarda el pedido en Neon y publica el evento en RabbitMQ (3 colas, cada una con su DLQ) y en Kafka (`pedidos.eventos`). `ms-analitica` consume `pedidos.eventos`, calcula métricas y las publica en `pedidos.metricas`. Los eventos inválidos van al tópico `pedidos.eventos.DLT`.

## Componentes

| Componente | Detalle |
|---|---|
| Frontend | Azure Static Web Apps (plan Free), HTTPS, despliegue automático con GitHub Actions al hacer push a `main` del repo del frontend. URL: `https://wonderful-sea-08ec42110.1.azurestaticapps.net` |
| Identidad (IDaaS) | Microsoft Entra ID: registro del frontend (SPA, PKCE) y registro de la API con el scope `access_as_user`. Redirect URIs tipo SPA: `http://localhost:4200` y la URL de Azure |
| API Manager | AWS API Gateway (HTTP API) `pedidos360-api`, URL `https://mxec3681gd.execute-api.us-east-1.amazonaws.com` |
| Autorizador | JWT de Entra ID (issuer v2.0 del tenant, audiencia = ID de la API). Sin token o con token alterado responde `401` |
| Rutas | `ANY /api/{proxy+}` con autorizador; `OPTIONS /api/{proxy+}` sin autorizador (preflight de CORS). Ambas llaman a `ms-pedidos` (8080) |
| Servidor | EC2 `t3.large` (8 GiB), Ubuntu 24.04 LTS, región `us-east-1`, Elastic IP. El lab no permite `t3.xlarge`; la `t3.medium` anterior (4 GiB) no alcanza con Kafka |
| RabbitMQ | 2 nodos en clúster con `infra/docker-compose.yml` (política `unless-stopped`) |
| Kafka | 3 Zookeeper y 3 brokers con `infra/kafka/docker-compose.yml` (política `unless-stopped`) |
| Kafka UI | Contenedor con `infra/kafka/docker-compose.kafka-ui.yml`, puerto 8090, accesible solo desde «Mi IP» |
| Tópicos Kafka | `pedidos.eventos` y `pedidos.metricas` (3 particiones, factor de réplica 3) y `pedidos.eventos.DLT` |
| Consumer groups | `analitica-metricas` y `analitica-auditoria` sobre `pedidos.eventos` |
| Microservicios | 7 servicios `systemd` (`Restart=on-failure`, habilitados al arranque, heap limitado con `-Xmx`) |
| Base de datos | PostgreSQL en Neon (el cómputo gratuito se apaga solo; la primera conexión tarda unos segundos) |

Con todo en marcha hay 9 contenedores (2 RabbitMQ, 3 Zookeeper, 3 Kafka y Kafka UI) y 7 servicios Java. Uso de memoria medido: unos 2,4 GB en contenedores y unos 3,8 GB en total con los servicios, con el swap casi sin uso.

## Seguridad

- **Security Group:**
  - SSH (22): solo desde «Mi IP».
  - Kafka UI (8090): solo desde «Mi IP».
  - 8080-8081: abiertos a `0.0.0.0/0`, porque el API Gateway no tiene IP fija y llama a `ms-pedidos` por la IP pública (si se cierran, el gateway responde `503`).
  - Cerrados desde internet: 9092 (Kafka), 5672 y 15672 (RabbitMQ) y 8082 a 8086 (uso interno).
- **JWT en dos capas:** el API Gateway lo valida primero y cada microservicio lo vuelve a validar (`oauth2ResourceServer`).
- **Credenciales fuera del repo:** `~/pedidos360.env` en la VM (`chmod 600`), cargado por `systemd` con `EnvironmentFile=`. Variables: `DB_URL`, `DB_USER`, `DB_PASS` y `RABBITMQ_HOST/PORT/USER/PASS`.
- **CORS:** se configura en el API Gateway y también en `CorsConfig.java` y `SecurityConfig.java` de `ms-pedidos`. Al agregar un nuevo origen hay que actualizar los tres sitios y redesplegar `ms-pedidos`.

## Guía de reinicio

1. AWS Academy → **Start Lab** y esperar el indicador verde; abrir la consola.
2. **EC2 → Instancias →** `pedidos360-vm` **→ Estado de la instancia → Iniciar**; esperar 2/2 comprobaciones.
3. Conectarse por SSH desde PowerShell (comillas por los espacios de la ruta):
   `ssh -i "<RUTA>\CloudEV2.pem" ubuntu@<ELASTIC_IP>`
   Si da *timeout*, la IP pública del equipo cambió: editar en el Security Group las reglas de SSH y de Kafka UI (8090) y volver a poner «Mi IP».
4. Verificar en la VM (esperar 1 o 2 minutos tras el arranque):
   ```bash
   docker ps --format "table {{.Names}}\t{{.Status}}"
   docker ps --format "{{.Names}} {{.Status}}" | wc -l
   systemctl is-active ms-pedidos ms-rabbit-admin ms-notificaciones ms-tickets ms-documentos ms-analitica ms-kafka-admin
   for p in 8080 8081 8082 8083 8084 8085 8086; do echo -n "puerto $p: "; curl -s localhost:$p/actuator/health | cut -c1-60; echo; done
   ```
   Debe haber 9 contenedores `Up` (`rabbitmq-1`, `rabbitmq-2`, 3 Zookeeper, `kafka-1`, `kafka-2`, `kafka-3` y Kafka UI), 7 × `active` y los 7 puertos en `UP`. Los contenedores y los servicios vuelven solos tras el reinicio.
5. Si algún contenedor no subió, levantar su compose:
   ```bash
   cd ~/pedidos360-backend/infra && docker compose up -d
   cd ~/pedidos360-backend/infra/kafka && docker compose up -d
   ```
   Si Kafka UI no subió, levantar `docker-compose.kafka-ui.yml` desde la misma carpeta con `docker compose -f docker-compose.kafka-ui.yml up -d`.
   Si un servicio falló: `sudo systemctl restart <nombre>` y `sudo journalctl -u <nombre> -n 20 --no-pager`.
6. Despertar Neon antes de usar la app (la primera consulta puede tardar ~20 s).

## Actualizar un servicio en la VM

```bash
cd ~/pedidos360-backend && git pull
cd ms-pedidos && ./mvnw -q clean package -DskipTests
sudo systemctl restart ms-pedidos
curl -s localhost:8080/actuator/health
```

Los demás servicios se actualizan igual, cambiando el nombre de la carpeta y del servicio. Al desplegar desde cero con RabbitMQ, levantar `ms-pedidos` primero: es quien declara las colas y exchanges al abrir la primera conexión. Con Kafka el orden de arranque no importa, porque los tópicos se crean aparte.

## Verificación rápida

- `GET /api/pedidos` por el gateway **sin token** → `401`; con token alterado → `401`; con token real → `200`.
- Clúster RabbitMQ: `docker exec rabbitmq-1 rabbitmqctl cluster_status` (2 nodos de disco y 2 en ejecución).
- Exchanges: `docker exec rabbitmq-1 rabbitmqctl list_exchanges name type | grep pedidos` (`pedidos.direct`, `pedidos.topic`, `pedidos.dlx`).
- Colas: `docker exec rabbitmq-1 rabbitmqctl list_queues name messages consumers` (3 colas principales y 3 `.dlq`).
- Kafka: en Kafka UI (`http://<ELASTIC_IP>:8090`) revisar **Topics** (`pedidos.eventos`, `pedidos.metricas` y `pedidos.eventos.DLT`, con réplicas en sincronía) y **Consumers** (`analitica-metricas` y `analitica-auditoria` en estado `STABLE` y lag 0).
- Flujo completo: un `POST /api/pedidos` por el gateway devuelve `201`; los 3 consumidores de RabbitMQ registran `ACK` y `ms-analitica` publica la métrica en `pedidos.metricas`.
- Mensaje fallido en RabbitMQ: un `POST /api/pedidos` con `cantidad = 0` genera en los 3 consumidores `NACK -> DLQ (irrecuperable)` y el registro `MENSAJE MUERTO` del listener de DLQ.
- Mensaje fallido en Kafka: ese mismo pedido (`cantidad = 0`) queda en `pedidos.eventos.DLT`, y un mensaje que no es JSON, publicado a mano desde Kafka UI en `pedidos.eventos`, también llega al DLT.

## Apagado

- Detener la instancia desde **EC2 → Estado de la instancia → Detener** (nunca «Terminar»). Los contenedores y los servicios vuelven solos al encenderla.
- La Elastic IP sigue cobrando aunque la instancia esté detenida; se libera solo cuando termine el proyecto.
- Si la IP cambia, hay que actualizar la integración del API Gateway (`ANY http://<ELASTIC_IP>:8080/api/{proxy}`).
- Neon, Azure Static Web Apps y Entra ID no necesitan apagarse.
