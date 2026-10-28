# Revisión OpenAPI del Sprint 6

Revisión del 22 de octubre de 2026 sobre `develop` en
`ba272a9457b1d14b7a11a0dcdba37767d37a9b56`, después de los PR #237, #238,
#239 y #241. Seguimiento: #214.
Swagger generado por los controllers sigue siendo la fuente de contratos;
este documento registra cobertura, evidencias y pendientes sin duplicar el catálogo de rutas.

## Estado de las dependencias

| Área | Implementación disponible | Resultado de la auditoría |
|---|---|---|
| Inasistencias y levantamientos | #196, PR #229 integrado | Roles y 400/401/403/404/409 contrastados con SecurityConfig y servicio; se aclaran el corte temporal, la independencia de lista negra y el error al agendar |
| Esquema y planes/sesiones | #199/#200, PR #228/#231 integrados | Contratos reales auditados; se completan 400 por filtros inválidos y 404 por perfil DOCTOR ausente; avance, saldo y límites de sesiones documentados |
| Abonos y Mis planes | #201, PR #237 integrado | Monto obligatorio positivo, 409 por sobrepago, bloqueo del plan y saldo coherente; ADMIN/SECRETARIA registran, DOCTOR consulta solo sus planes, CLIENTE usa su identidad JWT en Mis planes |
| Cinco reportes | #205/#206/#207 y PR #239 integrados | ADMIN/SECRETARIA; fechas inclusivas y corte de citas futuras según hora de Guatemala. Ganancias suma tratamientos de citas ATENDIDA por doctor y presenta abonos aparte por fecha de abono, sin sumarlos a tratamientos ni afirmar utilidad neta |
| Catálogo administrable | #223, PR #226 integrado | ADMIN modifica e incluye inactivos; filtro activo por defecto, errores, conservación de costos y rechazo de nuevos registros inactivos auditados |
| Bloqueos administrativos | Implementación previa integrada | ADMIN/SECRETARIA gestionan; DOCTOR solo lee los suyos; CLIENTE rechazado; listado incluye periodos finalizados |
| Bloqueos vigentes | #220, PR #239 integrado; issue aún abierto al revisar | Cualquier rol autenticado, incluido CLIENTE; solo inicio, fin y motivo, sin identificador administrativo; fin estrictamente posterior a ahora, incluye periodos en curso y futuros, ordenados por inicio; 400/401/404 documentados |

#201 y #220 ya están implementados en `develop`. El PR #241 integra las pantallas de
planes, reportes y avisos de disponibilidad; no se reimplementan esas funcionalidades.
Esta auditoría solo precisa anotaciones y amplía pruebas del contrato publicado.

## Comprobaciones locales y de CI

- Java 21 y PostgreSQL 16 aislado en memoria, sin usar bases reales ni credenciales de producción.
- La prueba general compara en ambos sentidos MVC y OpenAPI y comprueba JWT, respuestas,
  ausencia de cuerpo en 204 y schema compartido correcto para los errores documentados.
- Se verifican los tipos exactos de respuesta de inasistencias/levantamientos, planes/sesiones,
  los cinco reportes, catálogo y ambos listados de bloqueos; se conservan los ejemplos específicos de error.
- Fechas requeridas, ejemplos DTO, límites de sesiones/motivo y estados reales quedan comprobados.
- La prueba de abonos exige las operaciones ya integradas, sus schemas, monto obligatorio positivo
  y ausencia de identificador de paciente en Mis planes. Ya no omite estas comprobaciones si falta la ruta.
- La cobertura nueva comprueba campos y ejemplos de bloqueos vigentes y ganancias, UUID requerido
  del doctor, errores de bloqueos, permisos documentados y ejemplo 400 de rango invertido.
  Inicio y fin son strings de fecha/hora local de Guatemala sin offset, como los DTO reales.
- Se aclara que ganancias excluye estados distintos de ATENDIDA: una cita pasada sin marcar
  no aporta ingresos a ese reporte. No se modifica el cálculo ni la seguridad.
- Swagger UI y `/v3/api-docs` se comprueban mediante MockMvc. Una prueba con encabezados
  forwarded verifica el servidor, redirección de UI y URL del documento bajo `/api/`, conforme
  al nginx y `server.forward-headers-strategy=framework` actuales. Es una comprobación local,
  no una verificación del proxy real de EC2.
- Las pruebas de negocio, permisos y concurrencia permanecen en sus suites de integración;
  la auditoría de documentación no las duplica ni modifica reglas de negocio.
- Los resultados y enlaces de CI de cada commit quedan registrados en sus PRs.
- Resultado final local de esta revisión: `./mvnw -B verify` con Java 21 y PostgreSQL 16.15
  temporal: **280 pruebas, 0 fallos, 0 errores, 0 omitidas**, incluidas 7 de OpenAPI.
  Backend iniciado por HTTP local: Swagger UI, su bundle y `/v3/api-docs` responden 200;
  el documento publica 55 operaciones y swagger-config conserva el prefijo forwarded `/api/`.
  `git diff --check` sin errores. No se repite manualmente el flujo interactivo Authorize;
  JWT y acceso por roles se verifican en las suites de integración existentes.

## CI de la base integrada

- PR #239: [CI posterior al merge, exitoso](https://github.com/TEO1Group/TEO1_ClinicaDental/actions/runs/39799797934),
  commit `d0026a851cfb9b6ce907f1d935bfa533c305ac56`.
- PR #241: [CI posterior al merge, exitoso](https://github.com/TEO1Group/TEO1_ClinicaDental/actions/runs/39800006762),
  commit `ba272a9457b1d14b7a11a0dcdba37767d37a9b56`. Maven, pruebas/build Angular y ambos builds Docker completados.

## Pendientes para completar #214

1. Revisar e integrar las precisiones documentales de esta auditoría con CI exitoso.
2. Esperar el release #213 a cargo de Luis: respaldo y migraciones autorizadas antes del
   backend nuevo, dado que `ddl-auto=validate` no crea ni actualiza el esquema.
3. Con un despliegue autorizado y comprobable, consultar el documento OpenAPI y Swagger UI
   de EC2 con el prefijo `/api/`; contrastar operaciones y permisos con el commit desplegado
   y verificar que el CD corresponde a ese SHA. Usar la dirección confirmada por el release,
   sin asumir IP. Registrar SHA aprobado en main, run de CD exitoso y referencias de imágenes
   backend/frontend correspondientes a ese SHA, junto con el documento y las respuestas HTTP
   observadas en `/api/swagger-ui.html` y `/api/v3/api-docs`.

Al revisar, #213 sigue abierto y sin evidencias publicadas de ejecución; `origin/main` continúa
en `34c8a6b6ef595e01cb5b9b51fe24a8964558fd42`. No existe evidencia de que el release
Sprint 6 esté desplegado. La dependencia de implementación de #220 está resuelta; la verificación
de su contrato en producción depende del release, igual que el resto de operaciones nuevas.

#214 permanece abierto hasta completar estos criterios. Esta revisión no ejecuta despliegues,
migraciones ni acciones sobre EC2 y no certifica que esta versión esté en `main` o en producción.
