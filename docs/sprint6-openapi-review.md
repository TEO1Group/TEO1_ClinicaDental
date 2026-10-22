# Revisión OpenAPI del Sprint 6

Revisión complementaria del 22 de octubre de 2026 sobre `develop` en
`b24b7858a5c9f778f3ee16917c9329d0132e6d7a`. Seguimiento: #214.
Swagger generado por los controllers sigue siendo la fuente de contratos;
este documento registra cobertura, evidencias y pendientes sin duplicar el catálogo de rutas.

## Estado de las dependencias

| Área | Implementación disponible | Resultado de la auditoría |
|---|---|---|
| Inasistencias y levantamientos | #196, PR #229 integrado | Roles y 400/401/403/404/409 contrastados con SecurityConfig y servicio; se aclaran el corte temporal, la independencia de lista negra y el error al agendar |
| Esquema y planes/sesiones | #199/#200, PR #228/#231 integrados | Contratos reales auditados; se completan 400 por filtros inválidos y 404 por perfil DOCTOR ausente; avance, saldo y límites de sesiones documentados |
| Abonos y Mis planes | #201 implementado en PR #237, CI exitoso, pendiente de revisión e integración | Reutiliza entidades y cálculos existentes; JWT, montos, saldo, bloqueo transaccional y permisos verificados en sus pruebas; comprobación conjunta de OpenAPI con este PR en copia temporal |
| Cuatro reportes | #205/#206/#207, PR #232/#225 integrados | Fechas inclusivas, estados, agrupaciones y schemas contrastados; ingresos usa costo cobrado y fecha de cita, sin sumar abonos de planes |
| Catálogo administrable | #223, PR #226 integrado | ADMIN modifica e incluye inactivos; filtro activo por defecto, errores, conservación de costos y rechazo de nuevos registros inactivos auditados |
| Bloqueos administrativos | Implementación previa integrada | ADMIN/SECRETARIA gestionan; DOCTOR solo lee los suyos; CLIENTE rechazado; listado incluye periodos finalizados |
| Bloqueos vigentes | #220 aún sin controller en la base revisada | Pendiente externo; no se publica ni se simula esa operación |

#201 ya no está bloqueado por #199/#200: ambas dependencias están integradas.
El PR #237 aporta abonos y lectura de planes propios sin entidades ni migraciones duplicadas.
La disponibilidad en `develop` depende de que el equipo revise e integre ese PR.

## Comprobaciones locales y de CI

- Java 21 y PostgreSQL 16 aislado en memoria, sin usar bases reales ni credenciales de producción.
- La prueba general compara en ambos sentidos MVC y OpenAPI y comprueba JWT, respuestas,
  ausencia de cuerpo en 204 y schema compartido correcto para los errores documentados.
- Se verifican los tipos exactos de respuesta de inasistencias/levantamientos, planes/sesiones,
  los cuatro reportes, catálogo y bloqueos; se conservan los ejemplos específicos de error.
- Fechas requeridas, ejemplos DTO, límites de sesiones/motivo y estados reales quedan comprobados.
- La prueba de abonos verifica que no se documenten parcialmente operaciones inexistentes si
  #237 no está integrado. Cuando está presente, comprueba schemas, monto obligatorio positivo
  y ausencia de identificador de paciente en la operación Mis planes. Se ejecuta ese camino
  sobre una copia temporal que combina ambos parches, sin hacer merge ni modificar las ramas.
- Swagger UI y `/v3/api-docs` se comprueban mediante MockMvc. Una prueba con encabezados
  forwarded verifica el servidor, redirección de UI y URL del documento bajo `/api/`, conforme
  al nginx y `server.forward-headers-strategy=framework` actuales. Es una comprobación local,
  no una verificación del proxy real de EC2.
- Las pruebas de negocio, permisos y concurrencia permanecen en sus suites de integración;
  la auditoría de documentación no las duplica ni modifica reglas de negocio.
- Los resultados y enlaces de CI de cada commit quedan registrados en sus PRs.

## Pendientes para completar #214

1. Integrar #237 tras revisión y volver a ejecutar la suite en `develop`.
2. Auditar #220 si el equipo implementa bloqueos vigentes antes del release.
3. Esperar el release #213 a cargo de Luis: respaldo y migraciones autorizadas antes del
   backend nuevo, dado que `ddl-auto=validate` no crea ni actualiza el esquema.
4. Con un despliegue autorizado y comprobable, consultar el documento OpenAPI y Swagger UI
   de EC2 con el prefijo `/api/`; contrastar operaciones y permisos con el commit desplegado
   y verificar que el CD corresponde a ese SHA. No hay dirección IP asumida.

#214 permanece abierto hasta completar estos criterios. Esta revisión no ejecuta despliegues,
migraciones ni acciones sobre EC2 y no certifica que esta versión esté en `main` o en producción.
