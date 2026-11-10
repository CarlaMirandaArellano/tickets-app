# Guía paso a paso: AWS Academy + GitHub Actions

Sigue estos pasos en orden. Todo se hace en la región **us-east-1 (N. Virginia)**, que es la de AWS Academy.

---

## 1. Crear la instancia EC2

1. En **EC2 → Launch instance**:
   - **Name:** `tickets-app`
   - **AMI:** Amazon Linux 2023
   - **Instance type:** `t3.small` (también funciona `t2.micro`, pero va más justo de memoria)
   - **Key pair:** `vockey` (descarga `labsuser.pem` desde *AWS Details* en el Learner Lab) o crea uno nuevo
   - **Network settings:** VPC por defecto. Crea un security group `tickets-sg` con estas reglas de entrada:
     - SSH (22) desde *Anywhere* (lo necesita GitHub Actions)
     - Custom TCP **8080** desde *Anywhere* (para Postman)
   - **Advanced details → IAM instance profile:** **`LabInstanceProfile`**. Con esto la aplicación accede a S3 sin claves.
2. Lanza la instancia.

### Asignar una IP elástica (IP fija)

En el Learner Lab la EC2 se apaga al terminar cada sesión, y al volver a encenderla cambia la IP pública. Con una IP elástica la dirección queda fija.

1. Entra a **EC2 → Network & Security → Elastic IPs → Allocate Elastic IP address**, deja los valores por defecto y haz clic en **Allocate**.
2. Selecciónala y ve a **Actions → Associate Elastic IP address**. Elige *Resource type: Instance*, selecciona `tickets-app` y haz clic en **Associate**.
3. Usa esa IP para SSH, para el secret `EC2_HOST` y para la `baseUrl` de Postman.

> Costo: unos 0,005 USD por hora (≈ 3,65 USD/mes), incluso con la instancia apagada. Al terminar el ramo, haz **Disassociate** y luego **Release**.2. Lanza la instancia.

### Asignar una IP elástica (IP fija)

En el Learner Lab la EC2 se apaga al terminar cada sesión, y al volver a encenderla cambia la IP pública. Con una IP elástica la dirección queda fija.

1. Entra a **EC2 → Network & Security → Elastic IPs → Allocate Elastic IP address**, deja los valores por defecto y haz clic en **Allocate**.
2. Selecciónala y ve a **Actions → Associate Elastic IP address**. Elige *Resource type: Instance*, selecciona `tickets-app` y haz clic en **Associate**.
3. Usa esa IP para SSH, para el secret `EC2_HOST` y para la `baseUrl` de Postman.

> Costo: unos 0,005 USD por hora (≈ 3,65 USD/mes), incluso con la instancia apagada. Al terminar el ramo, haz **Disassociate** y luego **Release**.

### Instalar Docker y el cliente NFS

Conéctate por SSH (`ssh -i labsuser.pem ec2-user@IP`) y ejecuta:

```bash
sudo dnf update -y
sudo dnf install -y docker nfs-utils
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
```

### Permitir que el contenedor use el rol de la instancia

Prueba desde un contenedor que las credenciales del rol llegan:

```bash
sudo docker run --rm amazon/aws-cli sts get-caller-identity
```

- Si muestra un JSON con `"Arn": ...LabRole...`, está todo bien.
- Si falla por credenciales, ve a **EC2 → selecciona la instancia → Actions → Instance settings → Modify instance metadata options** y deja **Metadata response hop limit = 2**. Guarda y vuelve a probar.
- Si aun así no funciona, usa el plan B de la sección 5: credenciales como secrets.

---

## 2. Crear y montar el EFS

1. **EFS → Create file system**. Nombre `tickets-efs`, VPC por defecto → **Create**.
2. Abre el file system, ve a **Network → Manage** y, en cada zona de disponibilidad, agrega el security group **`tickets-sg`** (el de la EC2). Guarda.
3. En **EC2 → Security Groups → tickets-sg → Edit inbound rules**, agrega:
   - Tipo **NFS (2049)**, origen: **el mismo `tickets-sg`**. Sin esta regla el montaje se queda colgado.
4. En el EFS, haz clic en **Attach** y copia el **DNS name** (`fs-xxxxxxxx.efs.us-east-1.amazonaws.com`).
5. En la EC2:

```bash
sudo mkdir -p /mnt/efs
sudo mount -t nfs4 -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport \
  fs-xxxxxxxx.efs.us-east-1.amazonaws.com:/ /mnt/efs
df -h            # debe aparecer /mnt/efs
sudo chmod 777 /mnt/efs
```

6. (Opcional, recomendado) Para que el EFS se monte solo cuando la instancia se reinicia:

```bash
echo "fs-xxxxxxxx.efs.us-east-1.amazonaws.com:/ /mnt/efs nfs4 nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport,_netdev 0 0" | sudo tee -a /etc/fstab
```

De todos modos, el pipeline monta el EFS si detecta que no está montado.

---

## 3. Crear el bucket S3

1. **S3 → Create bucket**.
2. **Nombre:** debe ser único en todo AWS, por ejemplo `tickets-app-grupo5-2026`.
3. Región `us-east-1`. Deja **Block all public access** activado: los archivos se descargan a través de la API, no por URL pública.
4. **Create bucket**.

La aplicación crea las carpetas sola (`evento123/usuarioX/ticket001.pdf`).

---

## 4. Docker Hub

1. Crea una cuenta en <https://hub.docker.com> si no tienes.
2. **Account settings → Personal access tokens → Generate new token**, con permisos *Read & Write*. Copia el token.

No hace falta crear el repositorio: el primer push lo crea como `TU_USUARIO/tickets-app`.

---

## 5. Repositorio GitHub y secrets

1. Crea un repositorio y sube el proyecto a la rama **main**:

```bash
git init
git add .
git commit -m "Tickets App - Cloud Native"
git branch -M main
git remote add origin https://github.com/USUARIO/tickets-app.git
git push -u origin main
```

2. En **Settings → Secrets and variables → Actions → New repository secret**, crea:

| Secret | Valor |
|---|---|
| `DOCKERHUB_USERNAME` | Tu usuario de Docker Hub |
| `DOCKERHUB_TOKEN` | El token del paso 4 |
| `EC2_HOST` | IP elástica de la EC2 |
| `EC2_USER` | `ec2-user` |
| `EC2_SSH_KEY` | El contenido completo del `.pem` (incluidas las líneas BEGIN y END) |
| `EFS_DNS` | `fs-xxxxxxxx.efs.us-east-1.amazonaws.com` |
| `AWS_S3_BUCKET` | Nombre del bucket |
| `AWS_REGION` | `us-east-1` |

**Plan B (solo si el rol `LabInstanceProfile` no funciona desde el contenedor):** agrega también `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` y `AWS_SESSION_TOKEN`, que se copian desde *AWS Details → AWS CLI → Show* en el Learner Lab. **Estas credenciales vencen cuando termina la sesión del laboratorio**, así que hay que actualizarlas cada vez.

---

## 6. Ejecutar el pipeline

Cada `git push` a `main` dispara `.github/workflows/main.yml`. También se puede lanzar a mano desde **Actions → CI/CD - tickets-app → Run workflow**.

| Job | Qué hace |
|---|---|
| `test` | Compila con Maven y ejecuta las pruebas |
| `build-and-push` | Construye la imagen y la publica en Docker Hub (`latest` y el SHA del commit) |
| `deploy` | Entra por SSH a la EC2, monta el EFS si hace falta, baja la imagen nueva, reemplaza el contenedor con `-v /mnt/efs:/app/efs` y espera a que `/actuator/health` responda |

Para comprobar en la EC2:

```bash
sudo docker ps
sudo docker logs -f tickets-app
ls -R /mnt/efs/tickets          # los PDFs temporales
```

---

## 7. Probar con Postman

1. Importa `postman/tickets-app.postman_collection.json`.
2. En la colección, pestaña **Variables**, cambia `baseUrl` a `http://IP-EC2:8080`.
3. Ejecuta las carpetas en orden: 0 → 7. El request "Generar ticket…usuarioX" guarda el `ticketId` automáticamente.

---

## Problemas frecuentes

| Síntoma | Solución |
|---|---|
| El `mount` se queda colgado | Falta la regla NFS 2049 en `tickets-sg` o el EFS no tiene `tickets-sg` en sus mount targets |
| `502 Error de AWS S3 ... Unable to load credentials` | Revisa que la EC2 tenga `LabInstanceProfile` y hop limit 2, o usa el plan B |
| `409 No hay bucket S3 configurado` | Falta el secret `AWS_S3_BUCKET` |
| El deploy falla en SSH | Revisa que `EC2_HOST` tenga la IP elástica y que la instancia esté encendida (se apaga al terminar cada sesión del Learner Lab) |
| Postman no conecta | Falta abrir el puerto 8080 en el security group |
| La app se cae al iniciar en `t2.micro` | Usa `t3.small` o agrega `-e JAVA_TOOL_OPTIONS=-Xmx512m` al `docker run` |
