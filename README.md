# TEO1_ClinicaDental

Este proyecto tiene el objetivo de presentar una herramienta para el control de una clínica dental.

- Backend: Spring Boot (Java 21) en `backend/`
- Frontend: Angular en `clinica-dental-frontend/`
- Base de datos: PostgreSQL, el esquema está en `db/clinica_dental_schema.sql`
- Swagger/OpenAPI es la fuente principal y actualizada de documentación de la API.

## Levantar con Docker

1. Copiar `.env.example` a `.env` y llenar `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL` y `ADMIN_PASSWORD`. El `JWT_SECRET` va en base64, se puede generar con `openssl rand -base64 32`.
   Las variables `SMTP_*` son opcionales: si `SMTP_HOST` o `SMTP_FROM` quedan vacíos, el backend arranca igual y no envía los correos de recordatorio de citas.
2. Ejecutar:

```bash
docker compose up --build
```

- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- Swagger UI directo al backend: http://localhost:8080/swagger-ui.html
- OpenAPI JSON directo al backend: http://localhost:8080/v3/api-docs
- Swagger UI a través del proxy nginx: http://localhost:4200/api/swagger-ui.html
- OpenAPI JSON a través del proxy nginx: http://localhost:4200/api/v3/api-docs

Swagger/OpenAPI documenta las cinco operaciones implementadas por `CitaController`, además de las operaciones actuales de autenticación, usuarios, doctores y pacientes. No se publican operaciones que aún no existan en los controllers. El issue #98 necesita actualizar sus criterios: todavía solicita mantener citas en `ENDPOINTS.md`, que fue eliminado al consolidar Swagger como fuente principal.

En EC2, sustituye `<IP-PUBLICA-EC2>` por la IP pública asignada a la instancia:

- Swagger UI: `http://<IP-PUBLICA-EC2>:4200/api/swagger-ui.html`
- OpenAPI JSON: `http://<IP-PUBLICA-EC2>:4200/api/v3/api-docs`

En macOS, si el puerto 5432 ya está ocupado, configura solo en el `.env` local ignorado por Git `DB_HOST_PORT=5434` y `DB_URL=jdbc:postgresql://localhost:5434/clinica_dental`. El backend dentro de Docker sigue conectándose a `db:5432`; el puerto publicado por defecto continúa siendo 5432.

## Levantar sin Docker

Con una base de datos PostgreSQL local que tenga el esquema cargado:

```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5432/clinica_dental DB_USERNAME=postgres DB_PASSWORD=... \
JWT_SECRET=... ADMIN_EMAIL=... ADMIN_PASSWORD=... ./mvnw spring-boot:run
```

```bash
cd clinica-dental-frontend
npm install
npm start
```

## Backend

Spring Boot 4 con Spring Security (JWT), JPA y PostgreSQL. El código está en `backend/src/main/java/com/teo1/clinicadental/`:

- `controller`: endpoints REST
- `service`: reglas de negocio
- `repository`: acceso a la base de datos
- `model`: entidades mapeadas al esquema SQL
- `dto`: datos que entran y salen de la API, con sus validaciones
- `security`: generación y validación del token JWT
- `config`: seguridad por rol, CORS y creación del administrador inicial
- `exception`: formato común de errores

Roles y permisos:

| Rol | Puede |
|-----|-------|
| ADMIN | Administrar usuarios, doctores y pacientes; consultar y gestionar citas; administrar historial clínico y horarios. |
| SECRETARIA | Administrar pacientes y horarios; consultar todas las citas, agendarlas y cancelarlas; gestionar la lista negra. |
| DOCTOR | Ver pacientes, agregar al historial clínico, manejar sus horarios y consultar/cerrar sus propias citas como atendidas o no asistidas. |
| CLIENTE | Registrarse, iniciar sesión, ver doctores, agendar y consultar sus propias citas, y cancelar las suyas. |

El token se valida en cada petición; si un usuario se desactiva, su token deja de servir al momento.
Los datos se guardan en PostgreSQL y se mantienen al reiniciar el backend.

Las pruebas necesitan una base de datos PostgreSQL con el esquema cargado. Con Compose, inicia la base de datos y luego exporta las variables de `.env` antes de ejecutar Maven (Maven no carga `.env` por sí solo):

```bash
docker compose up -d db
set -a
. ./.env
set +a
cd backend
./mvnw test
```

## Frontend

Angular 22 en `clinica-dental-frontend/`. Llama al backend con el prefijo `/api` (en desarrollo lo redirige `proxy.conf.json` y en Docker lo hace `nginx.conf`).

Pantallas actuales: login, registro de pacientes, dashboard, administración de usuarios, listado/detalle/edición de pacientes, historial clínico y lista negra según rol, listado y detalle de doctores con horarios, listado y agendamiento de citas, y campana con próximas citas. El backend implementa consulta de próximas citas y recordatorios por correo opcionales mediante SMTP.
Las rutas se protegen por rol con `rolGuard` y el token se agrega a cada petición con `auth.interceptor`.

Consulta Swagger/OpenAPI para ver los cuerpos, respuestas, errores y requisitos de autorización documentados por el backend.

## CI/CD

- `ci.yml`: compila y ejecuta las pruebas del backend y frontend en cada push y pull request.
- `cd.yml`: construye las imágenes, las publica en GHCR y despliega en EC2.

## Cambios de esquema

Las migraciones se aplican manualmente y por separado del despliegue de aplicaciones. El workflow de CD no modifica el esquema de PostgreSQL ni elimina el volumen `postgres_data`.

> **No ejecutes `db/clinica_dental_schema.sql` sobre EC2 ni sobre una base con datos.** Ese archivo contiene instrucciones `DROP TABLE` y es solo para inicializar una base vacía.

Realiza los pasos siguientes desde `~/TEO1_ClinicaDental` en el servidor, con acceso autorizado a la base. No actives `set -x` ni incluyas contraseñas en los comandos.

1. **Inspecciona antes de cambiar.** Confirma que `docker compose ps db` muestra la base esperada como activa. Abre una sesión `psql` para revisar `\d+ cita` y los estados existentes:

   ```bash
   docker compose exec -it db sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
   ```

   Antes de crear el índice parcial, busca horarios activos duplicados:

   ```sql
   SELECT id_doctor, fecha, hora, count(*)
   FROM cita
   WHERE estado <> 'CANCELADA'
   GROUP BY id_doctor, fecha, hora
   HAVING count(*) > 1;
   ```

   La consulta debe devolver cero filas. Si devuelve resultados, detén el procedimiento y resuelve el conflicto con el responsable de los datos; no borres ni combines citas automáticamente.

2. **Crea un respaldo antes de migrar.** El siguiente ejemplo guarda un archivo de modo privado en el servidor; el contenedor usa las credenciales ya configuradas por Compose y el comando no imprime valores secretos:

   ```bash
   set -euo pipefail
   umask 077
   backup_file="../clinica_dental_$(date +%Y%m%d_%H%M%S).dump"
   docker compose exec -T db sh -lc \
     'pg_dump --format=custom --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
     > "$backup_file"
   ```

3. **Valida el archivo de respaldo antes de continuar.** Esto comprueba que `pg_restore` puede leer su catálogo:

   ```bash
   docker compose exec -T db pg_restore --list - < "$backup_file" > /dev/null
   ```

   Conserva el archivo fuera del repositorio y verifica que esté incluido en el mecanismo de respaldo aprobado para EC2. Un archivo legible no demuestra por sí solo que pueda restaurarse con éxito; la restauración debe ensayarse en una base aislada.

4. **Aplica la migración con errores fatales.** Solo después del respaldo y las verificaciones previas:

   ```bash
   docker compose exec -T db sh -lc \
     'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' \
     < db/migrations/2026-10-sprint4-cita.sql
   ```

   La migración abre una transacción. `ON_ERROR_STOP=1` hace que `psql` termine ante un error SQL; PostgreSQL revierte la transacción al cerrar la conexión. Corrige la causa y vuelve a inspeccionar antes de reintentar.

5. **Verifica el resultado antes de desplegar el backend.** Confirma las columnas, estados y definición del índice:

   ```bash
   docker compose exec -T db sh -lc \
     'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' <<'SQL'
   \d+ cita
   SELECT estado, count(*) FROM cita GROUP BY estado ORDER BY estado;
   SELECT column_name, data_type, is_nullable, column_default
   FROM information_schema.columns
   WHERE table_name = 'cita' AND column_name = 'recordatorio_enviado';
   SELECT indexdef FROM pg_indexes
   WHERE tablename = 'cita' AND indexname = 'uq_cita_doctor_slot';
   SQL
   ```

   Compara el total de citas con la inspección previa. Los estados antiguos se transforman así: `COMPLETADA` a `ATENDIDA`, `INASISTENCIA` a `NO_ASISTIO` y `APLAZADA` a `AGENDADA`. No se eliminan filas. El índice parcial permite reutilizar horarios cancelados y conserva la unicidad de los demás.

6. **Despliega y verifica en este orden:** respaldo validado, migración aplicada, consultas posteriores correctas, despliegue de backend/frontend y comprobaciones de salud del CD. El backend usa `ddl-auto=validate`; no crea ni altera tablas automáticamente.

Si falla la migración, no despliegues el backend nuevo. La transacción evita cambios parciales de esta migración, pero no revierte acciones ejecutadas fuera de ella ni protege contra fallos de disco o intervención externa. Si se necesita restaurar, detén el cambio y coordina una recuperación autorizada: restaura primero el respaldo en una base separada y valida los datos antes de decidir si reemplazar la base activa. Restaurar encima de la base activa puede borrar escrituras posteriores, y el respaldo puede contener información personal; no lo copies a Git ni lo compartas sin autorización.

### Obtener el archivo de migración desde el commit aprobado

El checkout de `main` en EC2 puede no contener todavía la migración que está en `develop`. No cambies de rama ni hagas checkout del release para obtenerla. Después de aprobar el SHA de `develop` y el SHA-256 del archivo, puedes extraer esa versión exacta sin mover el checkout desplegado:

```bash
set -euo pipefail
umask 077
git fetch origin develop
migration_commit='<SHA-COMPLETO-APROBADO-DE-DEVELOP>'
git cat-file -e "${migration_commit}^{commit}"
migration_file="../2026-10-sprint4-cita-${migration_commit}.sql"
git show "${migration_commit}:db/migrations/2026-10-sprint4-cita.sql" > "$migration_file"
sha256sum "$migration_file"
```

Compara el SHA-256 con el valor comunicado en la revisión aprobada antes de continuar. Ejecuta el archivo extraído únicamente después de completar el preflight, respaldo y autorización descritos arriba. `git fetch` solo actualiza referencias/objetos de Git; no despliega ni cambia el checkout activo.

### Recuperación de backend y frontend

Antes de publicar imágenes nuevas, guarda una copia privada del `.env` actual fuera del repositorio. Contiene configuración sensible y debe conservar permisos restrictivos:

```bash
set -euo pipefail
umask 077
release_env_backup="../clinica_dental_env_$(date +%Y%m%d_%H%M%S).bak"
cp .env "$release_env_backup"
```

Si falla la comprobación de salud del backend o frontend, conserva los logs y no vuelvas a desplegar automáticamente. Para recuperar la versión anterior de aplicación, restaura esa copia, verifica que sus `BACKEND_IMAGE` y `FRONTEND_IMAGE` sean las imágenes anteriores y recrea solo esos dos servicios:

```bash
cp "$release_env_backup" .env
docker compose up -d --no-build --no-deps --force-recreate backend frontend
docker compose ps
```

La recreación usa las imágenes anteriores que dejó en caché el paso de `pull` del despliegue fallido; si alguna no está disponible localmente, vuelve a autenticar Docker en GHCR por el mecanismo aprobado antes de intentar descargarla. Confirma Swagger/OpenAPI, la interfaz y el acceso a PostgreSQL antes de reanudar el servicio. No reviertas automáticamente el esquema: una restauración de PostgreSQL es una acción separada que puede borrar escrituras posteriores y requiere decisión autorizada. La instancia EC2, sus logs y la base no se han validado mediante este procedimiento de documentación.
