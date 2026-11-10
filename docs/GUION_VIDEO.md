# Guion de la presentación en Teams (≈ 12–15 min)

Cada bloque indica qué criterio de la pauta cubre. Graben la reunión, compartan pantalla y **hagan todo en vivo**: es parte de lo que se evalúa.

**Antes de grabar:** inicien el Learner Lab, verifiquen que la EC2 está corriendo y que el EFS está montado (`df -h`), y dejen abiertos la consola de AWS (EFS, S3, EC2), GitHub, Docker Hub, Postman y una terminal SSH.

---

### 1. Introducción (1 min)
- Presenten al equipo y el caso: una startup que necesita un sistema de reservas de eventos con tickets.
- Muestren el diagrama de arquitectura del README: EC2 + contenedor + EFS + S3 + pipeline.

### 2. Pipeline CI/CD en vivo (3 min). Criterio 6 (20 pts)
- Hagan un cambio pequeño (por ejemplo, el texto del pie del PDF en `PdfTicketGenerator`) y luego `git push origin main`.
- En **GitHub → Actions**, muestren los tres jobs: `test` → `build-and-push` → `deploy`.
- En **Docker Hub**, muestren la imagen `tickets-app` con las etiquetas `latest` y el SHA.
- En la terminal SSH, ejecuten `sudo docker ps` y muestren el contenedor recién creado con `-v /mnt/efs:/app/efs`.
- Expliquen el `main.yml`: secrets, login, build, push, SSH y health check.

### 3. EFS configurado (2 min). Criterio 1 (15 pts)
- En la consola, muestren el file system EFS, su red y el security group.
- En la EC2, ejecuten `df -h`: `/mnt/efs` aparece montado como NFS.
- Ejecuten `sudo docker exec -it tickets-app df -h`: `/app/efs` está montado dentro del contenedor.

### 4. Generar tickets y subida automática a S3 (2 min). Criterios 1 y 2 (15 + 10 pts)
- En Postman, carpeta **1**, ejecuten los 3 POST (evento123/usuarioX, evento123/usuarioY, evento456/usuarioX).
- Muestren en la respuesta `rutaEfs`, `s3Key` y `subidoS3: true`.
- En la EC2, ejecuten `ls -R /mnt/efs/tickets`: los PDFs están en el EFS.
- En la consola de S3, abran el bucket y naveguen por `evento123/` → `usuarioX/` → `ticket001.pdf`.
- En Postman, ejecuten **Listar archivos del bucket** y **Subir todos los pendientes**.
- Muestren el request de error 400 para explicar las validaciones.

### 5. Descargar desde S3 (1,5 min). Criterio 4 (10 pts)
- **Descargar un ticket (PDF)** con *Send and Download*. Abran el PDF y muestren los datos.
- **Descargar tickets de un usuario (ZIP)** y **de un evento (ZIP)**. Abran el ZIP y muestren que mantiene las carpetas evento/usuario.

### 6. Modificar en S3 (2 min). Criterio 3 (15 pts)
- **Modificar asiento, tipo y estado**. Descarguen el PDF de nuevo y muestren que cambió. En S3, la fecha *Last modified* del objeto también cambia.
- **Cambiar de usuario**. En S3, el archivo se movió a `evento123/usuarioZ/` y ya no está en `usuarioX/`.

### 7. Historial de archivos (1,5 min). Criterio 5 (10 pts)
- Carpeta **5**: filtros por evento, usuario, estado + fechas, y pendientes de subir.
- **Historial de un ticket**: se ve toda su vida (GENERADO_EFS → SUBIDO_S3 → DESCARGADO → MODIFICADO).
- **Historial solo de subidas a S3**.

### 8. Estadísticas (1 min)
- **Estadísticas de todos los eventos** y **de un evento**. Expliquen los campos: pagados (ventas), reservados, cancelados, reservas activas, monto vendido y usuarios únicos.

### 9. Eliminar (1 min)
- **Eliminar ticket** y luego **Confirmar eliminación (404)**.
- En S3 y en `ls /mnt/efs/tickets`, el archivo ya no está. En el historial aparece la acción ELIMINADO.

### 10. Cierre (1 min). Criterio 7 (20 pts)
- Recorran la lista de requerimientos del caso y digan cómo se cumplió cada uno (pueden mostrar la tabla de endpoints del README).
- Mencionen las decisiones de diseño: EFS como almacenamiento temporal compartido y S3 como almacenamiento definitivo y económico; el rol de instancia en lugar de claves; el health check en el pipeline.

---

## Checklist de entrega
- [ ] Link del video de Teams
- [ ] Link del repositorio GitHub (con el historial de Actions en verde)
- [ ] Archivo .zip con la documentación (README, `docs/` y `postman/`)
