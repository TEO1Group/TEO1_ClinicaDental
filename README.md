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

## CI/CD

- `ci.yml`: compila y ejecuta las pruebas del backend y frontend en cada push y pull request.
- `cd.yml`: construye las imágenes, las publica en GHCR y despliega en EC2.
