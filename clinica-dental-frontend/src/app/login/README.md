# Login frontend

La pantalla de login se muestra en `/` y conserva el enlace hacia `/registro`.

## Funcionamiento

El formulario valida un correo electrónico y una contraseña antes de llamar a `AuthService.login()`.
El servicio envía la solicitud al endpoint `/auth/login` mediante el prefijo `/api` del frontend.

```json
POST /auth/login
{
  "email": "paciente@ejemplo.com",
  "password": "ClaveSegura1!"
}
```

La respuesta esperada es:

```json
{
  "token": "<JWT>"
}
```

El token se guarda en `localStorage` con la clave `token`. El interceptor existente lo lee y envía como `Authorization: Bearer <token>` en las peticiones protegidas.

Después de iniciar sesión correctamente, la aplicación carga `/auth/me` y redirige al dashboard.