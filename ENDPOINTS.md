# Endpoints backend

Todo corre en localhost:8080

Para casi todo, excepto /auth/registro y /auth/login, hay que mandar el header:
Authorization: Bearer <token>

El token lo da el login, ahi viene el rol (ADMIN, DOCTOR, SECRETARIA o CLIENTE).

Errores comunes en cualquier endpoint: 400 si el body esta mal, 401 si no mandaste token o esta vencido, 403 si tu rol no tiene permiso ahi, 404 si no existe, 409 si el email o dpi ya estan registrados.

## Auth

POST /auth/registro - publico, registra un cliente
body: dpi, nombre, apellido, email, password, telefono (opcional), direccion (opcional), fechaNacimiento (opcional)
la contrasena pide minimo 8 caracteres, mayuscula, minuscula, numero y caracter especial
responde 201

POST /auth/login - publico
body: email, password
responde 200 con el token

## Usuarios (solo ADMIN)

GET /admin/usuarios - lista todos los usuarios
POST /admin/usuarios - crea doctor, secretaria o admin (los clientes se registran solos, no aqui)
body: nombre, apellido, email, password, telefono, idRol (DOCTOR, SECRETARIA o ADMIN)
si es DOCTOR tambien pide: especialidad (obligatorio), numeroColegiado, numeroClinica
si es SECRETARIA tambien pide: turno (MANANA/TARDE/NOCHE), area (los dos opcionales)
GET /roles - lista los roles que existen

## Doctores

GET /doctores - cualquiera autenticado
GET /doctores/{id} - cualquiera autenticado
PUT /doctores/{id} - solo ADMIN
GET /doctores/{id}/horarios - cualquiera autenticado
POST /doctores/{id}/horarios - ADMIN o el mismo DOCTOR
body: diaSemana (LUNES a DOMINGO), horaInicio, horaFin

## Pacientes

GET /pacientes - ADMIN, DOCTOR o SECRETARIA
GET /pacientes/{id} - ADMIN, DOCTOR o SECRETARIA
PUT /pacientes/{id} - ADMIN o SECRETARIA (actualiza telefono, direccion, fechaNacimiento)
PATCH /pacientes/{id}/lista-negra - SOLO SECRETARIA
body: enListaNegra (true/false), motivo (obligatorio si es true)
GET /pacientes/{id}/historial - ADMIN, DOCTOR o SECRETARIA
POST /pacientes/{id}/historial - ADMIN o DOCTOR
body: descripcion (la fecha la pone el server solo)
