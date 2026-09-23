# Endpoints backend

Todo corre en localhost:8080 (desde el frontend se llama con /api, el proxy lo manda al backend)

Para casi todo, excepto /auth/registro y /auth/login, hay que mandar el header:
Authorization: Bearer <token>

El token lo da el login, ahi viene el rol (ADMIN, DOCTOR, SECRETARIA o CLIENTE).

## Errores

Todos los errores vienen con este formato:

```json
{
  "status": 400,
  "error": "Bad Request",
  "mensaje": "telefono: El teléfono debe tener 8 dígitos",
  "path": "/pacientes",
  "errores": { "telefono": "El teléfono debe tener 8 dígitos" }
}
```

- mensaje: texto listo para mostrar en pantalla (error.error?.mensaje en Angular)
- errores: solo viene en los 400 de validacion, trae el mensaje de cada campo que fallo

Codigos: 400 si el body esta mal, 401 si no mandaste token, esta vencido o el usuario fue desactivado, 403 si tu rol no tiene permiso ahi, 404 si no existe, 409 si el email o dpi ya estan registrados o el horario choca con otro.

## Auth

POST /auth/registro - publico, registra un cliente
body: dpi, nombre, apellido, email, password, telefono (opcional), direccion (opcional), fechaNacimiento (opcional)
la contrasena pide minimo 8 caracteres, mayuscula, minuscula, numero y caracter especial
responde 201

POST /auth/login - publico
body: email, password
responde 200 con el token

GET /auth/me - cualquiera autenticado
devuelve los datos del usuario logueado: idUsuario, nombre, apellido, email, telefono, rol, idPerfil
idPerfil es el idCliente, idDoctor o idSecretaria segun el rol (null para ADMIN)
sirve para el dashboard y para que el doctor sepa su idDoctor al manejar sus horarios

## Administrador inicial

Al arrancar, si no hay ningun ADMIN en la base de datos, el backend crea uno con ADMIN_EMAIL y ADMIN_PASSWORD del .env.
Si ya existe un ADMIN o las variables estan vacias no hace nada.

## Usuarios (solo ADMIN)

GET /admin/usuarios - lista todos los usuarios (incluye los inactivos, trae el campo estado)
POST /admin/usuarios - crea doctor, secretaria o admin (los clientes se registran solos o los crea la secretaria en /pacientes)
body: nombre, apellido, email, password, telefono, idRol (DOCTOR, SECRETARIA o ADMIN)
si es DOCTOR tambien pide: especialidad (obligatorio), numeroColegiado, numeroClinica
si es SECRETARIA tambien pide: turno (MANANA/TARDE/NOCHE), area (los dos opcionales)
PUT /admin/usuarios/{id} - edita nombre, apellido, email y telefono (todos opcionales, solo cambia lo que mandes)
PATCH /admin/usuarios/{id}/estado - activa o desactiva un usuario
body: estado (ACTIVO o INACTIVO)
un usuario INACTIVO no puede iniciar sesion y su token deja de servir en ese momento (401)
el admin no puede desactivar su propia cuenta (400)
estos dos son para los botones de editar y cambiar estado del listado de usuarios
GET /roles - lista los roles que existen

## Doctores

GET /doctores - cualquiera autenticado, solo lista los doctores activos
GET /doctores/{id} - cualquiera autenticado
PUT /doctores/{id} - solo ADMIN
body: especialidad (obligatorio), numeroColegiado, numeroClinica, nombre, apellido, telefono (estos tres opcionales)
DELETE /doctores/{id} - solo ADMIN, desactiva al doctor (no lo borra, ya no puede iniciar sesion y sale del listado), responde 204

GET /doctores/{id}/horarios - cualquiera autenticado
POST /doctores/{id}/horarios - ADMIN, SECRETARIA o el mismo DOCTOR
PUT /doctores/{id}/horarios/{idHorario} - ADMIN, SECRETARIA o el mismo DOCTOR
DELETE /doctores/{id}/horarios/{idHorario} - ADMIN, SECRETARIA o el mismo DOCTOR, responde 204
body (POST y PUT): diaSemana (LUNES a DOMINGO), horaInicio, horaFin (formato "08:00")
un doctor no puede tocar horarios de otro doctor (403)
si el horario se cruza con otro del mismo dia responde 409 (08:00-12:00 y 12:00-14:00 si se permiten)

## Pacientes

GET /pacientes - ADMIN, DOCTOR o SECRETARIA, solo lista los pacientes activos
GET /pacientes/{id} - ADMIN, DOCTOR o SECRETARIA
POST /pacientes - ADMIN o SECRETARIA, crea un paciente desde recepcion
body: igual que /auth/registro, responde 201 con el paciente creado
PUT /pacientes/{id} - ADMIN o SECRETARIA (actualiza nombre, apellido, telefono, direccion, fechaNacimiento, todos opcionales)
DELETE /pacientes/{id} - ADMIN o SECRETARIA, desactiva al paciente (su historial se conserva), responde 204
PATCH /pacientes/{id}/lista-negra - SOLO SECRETARIA
body: enListaNegra (true/false), motivo (obligatorio si es true)
GET /pacientes/{id}/historial - ADMIN, DOCTOR o SECRETARIA
POST /pacientes/{id}/historial - ADMIN o DOCTOR
body: descripcion (la fecha la pone el server solo)

Las respuestas de doctor y paciente traen el campo estado (ACTIVO o INACTIVO).
