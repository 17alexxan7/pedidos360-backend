# Pedidos360 - Backend (DSY1107)

Microservicios Spring Boot (Java 21) con RabbitMQ (clúster de 2 nodos) y JWT de Microsoft Entra ID.

## Servicios

| Servicio | Puerto | Rol |
|---|---|---|
| `ms-pedidos` | 8080 | API de pedidos y productor de eventos |
| `ms-rabbit-admin` | 8081 | Administración de colas, exchanges y bindings |
| `ms-notificaciones` | 8082 | Consumidor de `cola.notificaciones` |
| `ms-tickets` | 8083 | Consumidor de `cola.tickets` |
| `ms-documentos` | 8084 | Consumidor de `cola.documentos` |

`infra/` contiene el `docker-compose.yml` y `rabbitmq.conf` del clúster RabbitMQ (2 nodos).

## Topología de mensajería

Los nombres viven en el bloque `app.messaging.*` de cada `application.yml`/`application.properties`.

| Dominio | Exchange | Routing key | Cola | DLQ (vía `pedidos.dlx`) |
|---|---|---|---|---|
| Notificaciones | `pedidos.topic` (topic) | `pedido.creado.#` | `cola.notificaciones` | `cola.notificaciones.dlq` (`notificaciones.dead`) |
| Tickets | `pedidos.direct` (direct) | `pedido.ticket` | `cola.tickets` | `cola.tickets.dlq` (`tickets.dead`) |
| Documentos | `pedidos.direct` (direct) | `pedido.documento` | `cola.documentos` | `cola.documentos.dlq` (`documentos.dead`) |

Al crear un pedido, `ms-pedidos` publica 3 mensajes persistentes (con publisher confirms).

## Política ACK / reintento / DLQ

Consumidores con `acknowledge-mode: manual` y `prefetch: 1`.

| Caso | Acción |
|---|---|
| Éxito | `basicAck` y log con `eventId` |
| `MensajeInvalidoException` (irrecuperable: cliente vacío o `cantidad <= 0`) | `basicNack(requeue=false)` directo a DLQ, sin reintentos |
| Otro error (recuperable) | Hasta 3 intentos con 2 s de espera; si se agotan, `basicNack(requeue=false)` a DLQ |

- Parámetros: `app.messaging.retry.max-attempts` (3) y `app.messaging.retry.backoff-ms` (2000).
- El reintento se hace en código: con ACK manual el `retry` del yml no se ejecuta.
- Cada fallo se registra con `slf4j` (eventId, motivo, reintento o DLQ).
- Cada servicio tiene un listener de DLQ que registra el mensaje muerto (`x-death`) y hace ACK. Se apaga con `--app.messaging.<dominio>.dlq-listener-enabled=false`.
- Demo: `producto = "FALLA-TEMPORAL-1"` (recuperable) y `cantidad = 0` (irrecuperable).

## API de ms-rabbit-admin (`/api/rabbitmq`, requiere JWT)

| Método | Ruta | Respuesta |
|---|---|---|
| POST | `/queues` | 201 |
| GET | `/queues/{name}` | 200 (`name`, `messageCount`, `consumerCount`) |
| DELETE | `/queues/{name}` | 204 |
| POST | `/exchanges` | 201 (tipo `DIRECT`, `TOPIC` o `FANOUT`) |
| DELETE | `/exchanges/{name}` | 204 |
| POST | `/bindings` | 201 |
| DELETE | `/bindings?queue=&exchange=&routingKey=` | 204 |

Errores: 400 (validación), 404 (no existe), 502 (falla RabbitMQ). Cuerpo: `{status, message, details}`.

## Ejecución local

1. `cd infra; docker compose up -d`
2. Levantar primero `ms-pedidos`, luego los demás servicios:
   `& "$env:JAVA_HOME\bin\java.exe" -jar <servicio>\target\<jar>`
3. Compilar: `.\mvnw.cmd clean package -DskipTests`

Credenciales de RabbitMQ y BD por variables de entorno (`RABBITMQ_HOST/PORT/USER/PASS`); no se versionan.

## Base de datos (ms-pedidos)

PostgreSQL en la nube (Neon). `ms-pedidos` persiste los pedidos con Spring Data JPA: entidad `Pedido` (tabla `pedidos`) y `ddl-auto=update`. Al arrancar con la tabla vacía inserta 2 pedidos de ejemplo, sin publicar eventos.

Variables de entorno (no se versionan):

| Variable | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host>/<base>?sslmode=require` |
| `DB_USER` | usuario de la BD |
| `DB_PASS` | clave de la BD |

En PowerShell: `$env:DB_URL = "..."; $env:DB_USER = "..."; $env:DB_PASS = "..."` antes de ejecutar el jar.

## Pruebas

`.\mvnw.cmd test` dentro de cada servicio (JUnit 5 + Mockito, sin RabbitMQ ni Entra ID). El test de contexto de `ms-pedidos` solo se ejecuta si `DB_URL` está definida.
## Despliegue en la nube
Ver [DESPLIEGUE.md](DESPLIEGUE.md).
