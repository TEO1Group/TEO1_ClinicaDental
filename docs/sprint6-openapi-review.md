# Revisión OpenAPI del Sprint 6

Revisión del 20 de octubre de 2026 sobre `develop` en
`3dc7518291af16d0a2b4375e86166d77a7b29219`. Seguimiento: #214.
Swagger generado por los controllers sigue siendo la fuente de contratos;
este documento registra cobertura y bloqueos, sin mantener otro catálogo de rutas.

## Cobertura comprobada

- Los controllers existentes documentan operaciones, JWT, respuestas y schemas.
  La prueba general compara en ambos sentidos las operaciones MVC reales y OpenAPI:
  detecta tanto operaciones sin documentación como operaciones documentadas sin implementación.
- El reporte actual conserva sus permisos ADMIN/SECRETARIA, las fechas inclusivas,
  estados y doctores activos. Se añade un ejemplo HTTP 400 específico de la operación,
  sin fijar errores ni códigos en el schema compartido `ErrorResponse`.
- El listado administrativo de bloqueos devuelve también periodos finalizados. Se
  aclara este comportamiento para no confundirlo con el futuro filtro de vigentes:
  ADMIN/SECRETARIA ven todos los doctores, DOCTOR solo los suyos, CLIENTE recibe 403.
- La prueba específica comprueba los schemas de respuesta del reporte y de bloqueos,
  y el ejemplo de error. No repite las reglas de negocio ya cubiertas por integración.
- Los tests existentes comprueban ejemplos DTO, los estados permitidos del PATCH,
  el esquema Bearer JWT y la carga de Swagger UI.

## Pendientes del equipo

| Área | Estado al revisar develop | Condición para completar #214 |
|---|---|---|
| Inasistencias | #196 sin implementación | Revisar reglas, schemas, errores y permisos tras integrarla |
| Planes | #199 y #200 sin tablas ni clases integradas | Auditar los contratos reales cuando se integren |
| Abonos y saldo | #201 bloqueado por #199/#200 | No crear entidades paralelas; esperar tablas, entidades, relación cita-plan y contrato del servicio de planes |
| Reportes adicionales | #205/#206 pendientes; #207 preparado en PR #225 | Revisar OpenAPI generado después de integrar cada implementación |
| Bloqueos vigentes | #220 sin implementación | Auditar su contrato público autenticado cuando exista; no confundir con el listado administrativo actual |
| Catálogo administrable | #223 preparado en PR #226 | Auditar en develop tras integrar; incluye sus propias anotaciones y pruebas |

#201 requiere además acordar los nombres y tipos de los campos del plan y abono,
la consulta del saldo y avance y el acceso transaccional a un plan para serializar
abonos concurrentes. No se ha inventado este contrato ni implementado un sustituto.

## Comprobación posterior al release

#214 permanece abierto. Cuando el equipo autorice y publique el release, consultar
el OpenAPI público de EC2 configurado con el prefijo `/api/`, contrastarlo con los
controllers del commit desplegado, comprobar Swagger UI y autorizaciones por rol,
y verificar que el CD corresponde a ese commit. No hay dirección IP asumida.
Esta revisión no ejecuta despliegues, migraciones ni acciones sobre EC2.
