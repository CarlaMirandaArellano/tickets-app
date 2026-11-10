# Tickets App: Sistema de Gestión de Reservas de Eventos con Tickets

Desarrollo Cloud Native (CDY2204) · Experiencia 1 · Semana 3
**"Construyendo y desplegando una solución Cloud Native"**

Es un microservicio en Spring Boot 3 (Java 17) que:

1. **Genera tickets personalizados en PDF** y los guarda en **Amazon EFS** como almacenamiento temporal.
2. **Los sube automáticamente a Amazon S3**, organizados por evento y usuario: `evento123/usuarioX/ticket001.pdf`.
3. Permite **descargar, modificar y eliminar** tickets, consultar el **historial de archivos** y obtener **estadísticas de ventas y reservas** por evento.
4. Se **despliega solo** con GitHub Actions: con cada push a `main`, el pipeline construye la imagen Docker, la publica en Docker Hub y la ejecuta en una EC2 con el EFS montado.

---

## Arquitectura

```
 Desarrollador ── git push main ──► GitHub Actions
                                       │ 1. mvn test
                                       │ 2. docker build + push ──► Docker Hub
                                       │ 3. SSH a EC2 ─┐
                                                       ▼
 Postman ── HTTP :8080 ──►  ┌──────────── EC2 (Amazon Linux 2023) ────────────┐
                            │  Contenedor tickets-app (Spring Boot)           │
                            │   /app/efs  ◄── -v ──  /mnt/efs ◄── NFS ── EFS  │
                            │      │  PDFs temporales + base H2               │
                            │      └── AWS SDK v2 ──────────────► S3 bucket   │
                            └─────────────────────────────────────────────────┘
                                         evento123/usuarioX/ticket001.pdf
```

| Componente | Uso |
|---|---|
| **EFS** | `/app/efs/tickets/{evento}/{usuario}/{ticket}.pdf`: archivo temporal recién generado. También guarda la base H2 (`/app/efs/db`), así los datos sobreviven a cada redespliegue. |
| **S3** | Almacenamiento definitivo: `{evento}/{usuario}/{ticket}.pdf`. |
| **Docker Hub** | Imagen `USUARIO/tickets-app:latest` y una etiqueta por commit. |
| **EC2** | Ejecuta el contenedor. El rol `LabInstanceProfile` le da acceso a S3 sin claves. |
| **GitHub Actions** | Pipeline CI/CD en `.github/workflows/main.yml`. |

---

## Endpoints

Base: `http://IP-EC2:8080`

| # | Requisito | Método y ruta | Descripción |
|---|---|---|---|
| 1 | Generar tickets | `POST /api/tickets` | Crea el ticket, genera el PDF en EFS y lo sube a S3 automáticamente |
| 2 | Subir a S3 | `POST /api/tickets/{id}/subir` | Sube (o vuelve a subir) un ticket desde el EFS a S3 |
|   |  | `POST /api/tickets/subir-pendientes` | Sube todos los tickets que aún no están en S3 |
|   |  | `GET /api/s3/archivos?prefijo=evento123/` | Lista los objetos reales del bucket |
| 3 | Descargar | `GET /api/tickets/{id}/descargar` | Descarga el PDF desde S3 |
|   |  | `GET /api/tickets/descargar?usuarioId=usuarioX` | ZIP con todos los tickets de un usuario |
|   |  | `GET /api/tickets/descargar?eventoId=evento123` | ZIP con todos los tickets de un evento |
| 4 | Modificar | `PUT /api/tickets/{id}` | Cambia los datos, regenera el PDF y lo reemplaza en S3 (si cambian el evento o el usuario, lo mueve de carpeta) |
| 5 | Eliminar | `DELETE /api/tickets/{id}` | Borra el ticket de S3, del EFS y de la base de datos |
| 6 | Estadísticas | `GET /api/estadisticas/eventos` | Ventas y reservas de todos los eventos |
|   |  | `GET /api/estadisticas/eventos/{eventoId}` | Ventas y reservas de un evento |
| – | Historial de archivos | `GET /api/tickets?eventoId=&usuarioId=&estado=&subidoS3=&desde=&hasta=` | Tickets generados, con filtros |
|   |  | `GET /api/historial?ticketId=&eventoId=&usuarioId=&accion=&desde=&hasta=` | Bitácora: GENERADO_EFS, SUBIDO_S3, MODIFICADO, DESCARGADO, ELIMINADO |
| – | Salud | `GET /actuator/health` | El pipeline la usa para verificar el despliegue |

### Ejemplo: generar un ticket

```json
POST /api/tickets
{
  "eventoId": "evento123",
  "eventoNombre": "Concierto Rock Duoc",
  "fechaEvento": "2026-12-15T21:00:00",
  "lugar": "Movistar Arena, Santiago",
  "usuarioId": "usuarioX",
  "usuarioNombre": "María González",
  "usuarioEmail": "usuariox@correo.cl",
  "tipoEntrada": "VIP",
  "asiento": "VIP-12",
  "precio": 25000,
  "estado": "PAGADO"
}
```

La respuesta incluye `codigo` (`ticket001`), `rutaEfs`, `s3Key` (`evento123/usuarioX/ticket001.pdf`) y `subidoS3: true`.

Los estados posibles son `RESERVADO` (valor por defecto), `PAGADO` (cuenta como venta) y `CANCELADO`.

---

## Variables de entorno

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `EFS_PATH` | `./efs` (en la imagen: `/app/efs`) | Carpeta donde está montado el EFS |
| `AWS_S3_BUCKET` | vacío | Nombre del bucket. Si está vacío, solo se usa el EFS |
| `AWS_REGION` | `us-east-1` | Región |
| `S3_AUTO_UPLOAD` | `true` | Sube a S3 automáticamente al generar o modificar |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | – | Opcionales: solo si no se usa `LabInstanceProfile` |

---

## Ejecutar en local

```bash
mvn spring-boot:run                       # solo EFS local (carpeta ./efs)
AWS_S3_BUCKET=mi-bucket mvn spring-boot:run   # con S3 (requiere credenciales de AWS Academy)
mvn test                                  # pruebas
```

Con Docker:

```bash
docker build -t tickets-app .
docker run -p 8080:8080 -v $(pwd)/efs:/app/efs -e AWS_S3_BUCKET=mi-bucket \
  -e AWS_ACCESS_KEY_ID=... -e AWS_SECRET_ACCESS_KEY=... -e AWS_SESSION_TOKEN=... tickets-app
```

---

## Estructura

```
tickets-app/
├── .github/workflows/main.yml     Pipeline CI/CD
├── Dockerfile                     Imagen multi-etapa (Maven → JRE 17)
├── pom.xml
├── postman/                       Colección de pruebas
├── docs/
│   ├── GUIA_AWS_Y_PIPELINE.md     Configuración paso a paso (EC2, EFS, S3, secrets)
│   └── GUION_VIDEO.md             Guion de la presentación alineado con la pauta
└── src/main/java/cl/duoc/tickets/
    ├── config/S3Config.java
    ├── controller/                TicketController, EstadisticasController, ArchivosController
    ├── dto/
    ├── exception/
    ├── model/                     Ticket, HistorialArchivo, EstadoTicket, AccionArchivo
    ├── repository/
    └── service/                   TicketService, EfsStorageService, S3StorageService,
                                   PdfTicketGenerator, EstadisticasService
```
