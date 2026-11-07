# tickets-service — Sistema de Gestión de Reservas de Eventos con Tickets

Microservicio Spring Boot (Java 21) para la actividad sumativa **"Construyendo y desplegando una solución Cloud Native"** (CDY2204, Semana 3).

## Arquitectura

```
 Postman ──HTTP:8080──▶ EC2 (Docker)
                         └─ contenedor tickets-service
                              │  1. genera el PDF del ticket
                              ├─▶ /app/efs  ◀── montado desde /mnt/efs ◀── Amazon EFS  (almacenamiento temporal + historial H2)
                              │  2. lo sube automáticamente
                              └─▶ Amazon S3   s3://<bucket>/{eventoId}/{usuarioId}/ticketNNN.pdf

 git push (main) ─▶ GitHub Actions: mvn verify ─▶ docker build ─▶ Docker Hub ─▶ SSH a EC2 ─▶ docker run -v /mnt/efs:/app/efs
```

| Componente | Uso |
|---|---|
| **Amazon EFS** | Los PDF se generan primero en el EFS (`/app/efs/tickets/{evento}/{usuario}/`). También guarda la base H2 con el historial, así no se pierde al redesplegar. |
| **Amazon S3** | Copia definitiva de los tickets, organizada por evento y usuario: `evento123/usuarioX/ticket001.pdf`. |
| **Docker Hub** | Registro de la imagen `<usuario>/tickets-service:latest` (y una etiqueta por commit). |
| **GitHub Actions** | Pipeline CI/CD en `.github/workflows/main.yml`. |
| **EC2** | Ejecuta el contenedor en el puerto 8080. |

## Endpoints REST

| # | Método | Ruta | Descripción |
|---|---|---|---|
| 1 | POST | `/api/tickets` | **Generar tickets** (PDF en EFS + subida automática a S3). Acepta `cantidad` (1-10). |
| 2 | POST | `/api/tickets/{id}/subir` | **Subir ticket a S3** desde el EFS (manual / reintento). |
| 2b | POST | `/api/tickets/subir-pendientes` | Sube todos los tickets que quedaron solo en el EFS. |
| 3 | GET | `/api/tickets/{id}/descargar` | **Descargar un ticket** (PDF) desde S3. |
| 3b | GET | `/api/tickets/descargar?eventoId=..&usuarioId=..` | **Descargar por usuario o evento** (ZIP desde S3). |
| 4 | PUT | `/api/tickets/{id}` | **Modificar detalles**: regenera el PDF en EFS y lo actualiza en S3 (si cambia evento/usuario, lo mueve de carpeta). |
| 5 | DELETE | `/api/tickets/{id}` | **Eliminar ticket**: borra el PDF del EFS y de S3; queda en el historial como `ELIMINADO`. |
| 6 | GET | `/api/estadisticas/eventos` | **Estadísticas de ventas y reservas** por evento. |
| 6b | GET | `/api/estadisticas/eventos/{eventoId}` | Estadísticas de un evento. |
| 7 | GET | `/api/tickets?eventoId=&usuarioId=&estado=&desde=&hasta=` | **Historial** de tickets generados con filtros combinables (`estado`: RESERVADO, VENDIDO, ELIMINADO; fechas `yyyy-MM-dd`). |
| — | GET | `/api/tickets/s3?prefijo=evento123/` | Lista los objetos reales del bucket. |
| — | GET | `/` y `/actuator/health` | Estado del servicio. |

Ejemplo de cuerpo para generar:

```json
{
  "eventoId": "evento123", "eventoNombre": "Concierto Duoc Rock",
  "fechaEvento": "2026-12-10T20:00:00", "lugar": "Movistar Arena",
  "usuarioId": "usuarioX", "usuarioNombre": "Ana Pérez", "usuarioEmail": "ana@correo.cl",
  "asiento": "A-10", "precio": 25000, "estado": "VENDIDO", "cantidad": 2
}
```

La colección de Postman está en `postman/tickets-service.postman_collection.json`.

## Variables de entorno

| Variable | Ejemplo | Descripción |
|---|---|---|
| `S3_BUCKET` | `tickets-eventos-mm-2026` | Nombre del bucket |
| `AWS_REGION` | `us-east-1` | Región |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN` | (del Learner Lab) | Credenciales; cambian en cada sesión del lab |
| `EFS_PATH` | `/app/efs` | Carpeta del EFS dentro del contenedor |
| `S3_AUTO_UPLOAD` | `true` | Subida automática a S3 al generar |

## Configuración en GitHub Actions

**Secrets** (datos sensibles): `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `USER_SERVER`, `EC2_SSH_KEY`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`.

**Variables** (configuración no sensible): `AWS_REGION` (`us-east-1`), `S3_BUCKET`, `DOCKER_IMAGE_NAME` (`tickets-service`).

## Ejecutar localmente

```bash
mvn spring-boot:run
```

Sin credenciales AWS los tickets quedan en `./efs-local` con `subidoS3: false`. Los tests (`mvn verify`) simulan S3 en memoria.

Despliegue demostrado en video - 07-11-2026
