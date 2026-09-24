# TEO1_ClinicaDental

Este proyecto tiene el objetivo de presentar una herramienta para el control de una clínica dental.

- Backend: Spring Boot (Java 21) en `backend/`
- Frontend: Angular en `clinica-dental-frontend/`
- Base de datos: PostgreSQL, el esquema está en `db/clinica_dental_schema.sql`
- Endpoints documentados en [ENDPOINTS.md](ENDPOINTS.md)

## Levantar con Docker

1. Copiar `.env.example` a `.env` y llenar `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL` y `ADMIN_PASSWORD`. El `JWT_SECRET` va en base64, se puede generar con `openssl rand -base64 32`.
2. Ejecutar:

```bash
docker compose up --build
```

- Frontend: http://localhost:4200
- Backend: http://localhost:8080

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
| ADMIN | Crear y editar usuarios, activar/desactivar cuentas, editar y desactivar doctores, pacientes, historial y horarios (la lista negra es solo de la secretaria) |
| SECRETARIA | Pacientes (crear, editar, desactivar, lista negra) y horarios de los doctores |
| DOCTOR | Ver pacientes, agregar al historial clínico y manejar sus propios horarios |
| CLIENTE | Registrarse, iniciar sesión y ver doctores |

El token se valida en cada petición; si un usuario se desactiva, su token deja de servir al momento.
Los datos se guardan en PostgreSQL y se mantienen al reiniciar el backend.

Las pruebas necesitan una base de datos PostgreSQL con el esquema cargado (así corren en el CI):

```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5432/clinica_dental DB_USERNAME=postgres DB_PASSWORD=... JWT_SECRET=... ./mvnw test
```

## Frontend

Angular 22 en `clinica-dental-frontend/`. Llama al backend con el prefijo `/api` (en desarrollo lo redirige `proxy.conf.json` y en Docker lo hace `nginx.conf`).

Pantallas actuales: login, registro de pacientes, dashboard, listado y creación de usuarios (admin), listado y detalle de doctores con sus horarios.
Las rutas se protegen por rol con `rolGuard` y el token se agrega a cada petición con `auth.interceptor`.

Lo que falta conectar está en la sección "Pendiente en frontend" de [ENDPOINTS.md](ENDPOINTS.md).

## CI/CD

- `ci.yml`: compila y ejecuta las pruebas del backend y frontend en cada push y pull request.
- `cd.yml`: construye las imágenes, las publica en GHCR y despliega en EC2.
