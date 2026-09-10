# Login frontend

La pantalla se muestra en `/` y conserva el enlace hacia `/registro`.

## Contrato preparado

Cuando el backend exponga el endpoint, `AuthService.login()` enviará:

```json
POST /auth/login
{
  "usuario": "1234567890123",
  "password": "ClaveSegura1!"
}
```

La respuesta esperada es:

```json
{
  "token": "<JWT>"
}
```

El token se guardará en `localStorage` con la clave `jwt`, que ya utiliza el interceptor existente para enviar `Authorization: Bearer <token>`.