-- Datos de demo de la Clínica Dental (Sprint 7). También sirve para reiniciar la demo: cada corrida la
-- deja como nueva.
--
-- Crea: 2 doctores con horarios, 1 secretaria, 5 pacientes (uno en lista negra), citas de ayer, hoy y
-- mañana en todos los estados, tratamientos registrados, una reprogramación, historial clínico y un
-- bloqueo de doctor. Las fechas se calculan al correr el script (zona America/Guatemala).
--
-- Citas en curso: las dos citas Agendadas de hoy empiezan entre 20 y 50 minutos antes de correr el
-- script, así el doctor las puede marcar en la demo a cualquier hora: la de Ana López (Dra. Morales)
-- para atenderla y registrar el tratamiento, y la de Luis Pérez (Dr. Castillo) para marcar No asistió
-- y ver cómo llega al límite de inasistencias. La cita Atendida de hoy es una hora antes.
--
-- Sprint 6:
--   - Inasistencias: Luis Pérez tiene 2 desde que le levantaron la restricción hace 40 días (antes tuvo 3);
--     Carmen Díaz tiene 3 y está bloqueada para agendar hasta que se le levante.
--   - Planes: Ana López, endodoncia de 3 sesiones con 2 atendidas, 1 agendada y 2 abonos (saldo Q2,000);
--     María Gómez, ortodoncia de 6 sesiones con 2 atendidas, 1 cancelada, su cita de mañana y 1 abono.
--   - Reportes: citas NO_ASISTIO, CANCELADA y ATENDIDA con tratamientos en las últimas semanas.
--
-- Requiere el esquema completo o las migraciones hasta 2026-10-sprint6-*.sql (usa el catálogo del Sprint 6).
--
-- La contraseña NO está en el repositorio: se pasa al ejecutar y se guarda con BCrypt (pgcrypto).
-- Todos los usuarios de demo usan esa misma contraseña.
--
-- Uso (sobre una base que ya tiene db/clinica_dental_schema.sql o las migraciones aplicadas):
--   psql -h <host> -U <usuario> -d <base> -v ON_ERROR_STOP=1 \
--        -v clave_demo='<contraseña de demo>' \
--        [-v correo_demo='<tu correo>'] \
--        -f db/demo/datos-demo.sql
--
-- correo_demo (opcional) reemplaza el correo de la paciente Ana López, para recibir el recordatorio
-- de su cita de mañana en un buzón real.
--
-- Se puede correr varias veces: primero borra los datos de demo anteriores (solo los suyos, por id).
-- Con -v solo_limpiar=1 (sin clave_demo) solo borra la demo y no carga nada.

\set ON_ERROR_STOP 1

\if :{?solo_limpiar}
\elif :{?clave_demo}
\else
    \echo 'Falta la contraseña: agrega -v clave_demo=''<contraseña>'' al comando psql'
    SELECT 1 / 0 AS falta_clave_demo;
\endif

\if :{?correo_demo}
\else
    \set correo_demo 'ana.lopez@clinica-demo.test'
\endif

-- Fechas relativas a hoy en Guatemala
SELECT (now() AT TIME ZONE 'America/Guatemala')::date - 1 AS ayer,
       (now() AT TIME ZONE 'America/Guatemala')::date AS hoy,
       (now() AT TIME ZONE 'America/Guatemala')::date + 1 AS manana,
       (now() AT TIME ZONE 'America/Guatemala')::date + 2 AS pasado_manana
\gset

-- Inicio de las citas en curso: el bloque de 30 minutos que empezó entre 20 y 50 minutos antes de ahora.
-- Si se corre poco después de medianoche, cae en la noche de ayer.
SELECT en_curso::date AS en_curso_fecha, en_curso::time AS en_curso_hora,
       (en_curso - interval '1 hour')::date AS antes_fecha, (en_curso - interval '1 hour')::time AS antes_hora
FROM (SELECT date_bin(interval '30 minutes', (now() AT TIME ZONE 'America/Guatemala') - interval '20 minutes',
                      timestamp '2000-01-01') AS en_curso) AS t
\gset

BEGIN;

-- 1. Limpiar la demo anterior. Al borrar los usuarios se borran en cascada sus perfiles, citas,
--    tratamientos de cita, reprogramaciones, historial, horarios y bloqueos.
DELETE FROM usuario WHERE id_usuario IN (
    'de000000-0000-4000-8000-000000000001', 'de000000-0000-4000-8000-000000000002',
    'de000000-0000-4000-8000-000000000003', 'de000000-0000-4000-8000-000000000011',
    'de000000-0000-4000-8000-000000000012', 'de000000-0000-4000-8000-000000000013',
    'de000000-0000-4000-8000-000000000014', 'de000000-0000-4000-8000-000000000015'
);

\if :{?solo_limpiar}
COMMIT;
\echo 'Datos de demo borrados; no se cargó nada.'
\quit
\endif

-- 2. Usuarios
INSERT INTO usuario (id_usuario, nombre, apellido, email, password_hash, id_rol, telefono, estado)
SELECT u.id::uuid, u.nombre, u.apellido, u.email, crypt(:'clave_demo', gen_salt('bf', 10)),
       (SELECT id_rol FROM rol WHERE nombre_rol = u.rol), u.telefono, 'ACTIVO'
FROM (VALUES
    ('de000000-0000-4000-8000-000000000001', 'Lucía',  'Morales',  'lucia.morales@clinica-demo.test',  'DOCTOR',     '55510001'),
    ('de000000-0000-4000-8000-000000000002', 'Andrés', 'Castillo', 'andres.castillo@clinica-demo.test','DOCTOR',     '55510002'),
    ('de000000-0000-4000-8000-000000000003', 'Sofía',  'Herrera',  'sofia.herrera@clinica-demo.test',  'SECRETARIA', '55510003'),
    ('de000000-0000-4000-8000-000000000011', 'Ana',    'López',    :'correo_demo',                     'CLIENTE',    '55520001'),
    ('de000000-0000-4000-8000-000000000012', 'Luis',   'Pérez',    'luis.perez@clinica-demo.test',     'CLIENTE',    '55520002'),
    ('de000000-0000-4000-8000-000000000013', 'María',  'Gómez',    'maria.gomez@clinica-demo.test',    'CLIENTE',    '55520003'),
    ('de000000-0000-4000-8000-000000000014', 'Jorge',  'Ruiz',     'jorge.ruiz@clinica-demo.test',     'CLIENTE',    '55520004'),
    ('de000000-0000-4000-8000-000000000015', 'Carmen', 'Díaz',     'carmen.diaz@clinica-demo.test',    'CLIENTE',    '55520005')
) AS u (id, nombre, apellido, email, rol, telefono);

-- 3. Perfiles
INSERT INTO doctor (id_doctor, id_usuario, especialidad, numero_colegiado, numero_clinica) VALUES
    ('de000000-0000-4000-8000-0000000000d1', 'de000000-0000-4000-8000-000000000001', 'Odontología general', 'COL-1001', '1'),
    ('de000000-0000-4000-8000-0000000000d2', 'de000000-0000-4000-8000-000000000002', 'Ortodoncia',          'COL-1002', '2');

INSERT INTO secretaria (id_secretaria, id_usuario, turno, area) VALUES
    ('de000000-0000-4000-8000-0000000000a1', 'de000000-0000-4000-8000-000000000003', 'MANANA', 'RECEPCION');

INSERT INTO cliente (id_cliente, id_usuario, dpi, direccion, fecha_nacimiento, en_lista_negra, motivo_lista_negra) VALUES
    ('de000000-0000-4000-8000-0000000000c1', 'de000000-0000-4000-8000-000000000011', '9900000000011', 'Zona 1, Guatemala',  '1995-04-12', FALSE, NULL),
    ('de000000-0000-4000-8000-0000000000c2', 'de000000-0000-4000-8000-000000000012', '9900000000012', 'Zona 10, Guatemala', '1988-09-30', FALSE, NULL),
    ('de000000-0000-4000-8000-0000000000c3', 'de000000-0000-4000-8000-000000000013', '9900000000013', 'Mixco',              '2001-01-05', FALSE, NULL),
    ('de000000-0000-4000-8000-0000000000c4', 'de000000-0000-4000-8000-000000000014', '9900000000014', 'Villa Nueva',        '1979-11-21', TRUE,
     'Tres inasistencias seguidas sin avisar'),
    ('de000000-0000-4000-8000-0000000000c5', 'de000000-0000-4000-8000-000000000015', '9900000000015', 'Zona 7, Guatemala',  '1992-06-17', FALSE, NULL);

-- 4. Horarios: todos los días, para que la demo funcione cualquier día de la semana
INSERT INTO horario (id_doctor, dia_semana, hora_inicio, hora_fin)
SELECT d.id_doctor::uuid, dia, d.inicio::time, d.fin::time
FROM (VALUES
    ('de000000-0000-4000-8000-0000000000d1', '08:00', '18:00'),
    ('de000000-0000-4000-8000-0000000000d2', '09:00', '17:00')
) AS d (id_doctor, inicio, fin)
CROSS JOIN unnest(ARRAY['LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO']) AS dia;

-- 5. Citas de ayer, hoy y mañana (D1 = Dra. Morales, D2 = Dr. Castillo)
INSERT INTO cita (id_cita, id_cliente, id_doctor, fecha, hora, estado, notas, motivo_estado) VALUES
    -- Ayer
    ('de000000-0000-4000-8000-00000000c101', 'de000000-0000-4000-8000-0000000000c1', 'de000000-0000-4000-8000-0000000000d1',
     :'ayer', '09:00', 'ATENDIDA', 'Revisión general', NULL),
    ('de000000-0000-4000-8000-00000000c102', 'de000000-0000-4000-8000-0000000000c2', 'de000000-0000-4000-8000-0000000000d1',
     :'ayer', '10:00', 'NO_ASISTIO', 'Dolor en muela', 'El paciente no se presentó ni avisó'),
    -- Hoy: una atendida una hora antes y las dos en curso (ver el encabezado)
    ('de000000-0000-4000-8000-00000000c201', 'de000000-0000-4000-8000-0000000000c3', 'de000000-0000-4000-8000-0000000000d1',
     :'antes_fecha', :'antes_hora', 'ATENDIDA', 'Caries en pieza 36', NULL),
    ('de000000-0000-4000-8000-00000000c202', 'de000000-0000-4000-8000-0000000000c1', 'de000000-0000-4000-8000-0000000000d1',
     :'en_curso_fecha', :'en_curso_hora', 'AGENDADA', 'Control después de la limpieza', NULL),
    ('de000000-0000-4000-8000-00000000c203', 'de000000-0000-4000-8000-0000000000c2', 'de000000-0000-4000-8000-0000000000d2',
     :'en_curso_fecha', :'en_curso_hora', 'AGENDADA', 'Evaluación para brackets', NULL),
    -- Mañana (la de Ana López es la que recibe el correo de recordatorio)
    ('de000000-0000-4000-8000-00000000c301', 'de000000-0000-4000-8000-0000000000c1', 'de000000-0000-4000-8000-0000000000d1',
     :'manana', '09:00', 'AGENDADA', 'Limpieza dental', NULL),
    ('de000000-0000-4000-8000-00000000c302', 'de000000-0000-4000-8000-0000000000c3', 'de000000-0000-4000-8000-0000000000d2',
     :'manana', '10:00', 'AGENDADA', 'Ajuste de brackets', NULL),
    ('de000000-0000-4000-8000-00000000c303', 'de000000-0000-4000-8000-0000000000c2', 'de000000-0000-4000-8000-0000000000d1',
     :'manana', '11:00', 'CANCELADA', 'Blanqueamiento', 'Viaje de trabajo');

-- 6. Tratamientos realizados en las citas atendidas (costo del catálogo, uno ajustado)
INSERT INTO cita_tratamiento (id_cita, id_tratamiento, costo, observaciones)
SELECT t.id_cita::uuid, tr.id_tratamiento, coalesce(t.costo, tr.costo), t.observaciones
FROM (VALUES
    ('de000000-0000-4000-8000-00000000c101', 'Consulta y diagnóstico', NULL::numeric, 'Sin hallazgos importantes'),
    ('de000000-0000-4000-8000-00000000c101', 'Limpieza dental',        NULL::numeric, 'Sarro moderado'),
    ('de000000-0000-4000-8000-00000000c201', 'Radiografía periapical', NULL::numeric, 'Pieza 36'),
    ('de000000-0000-4000-8000-00000000c201', 'Resina (empaste)',       450.00,        'Pieza 36, caries profunda')
) AS t (id_cita, tratamiento, costo, observaciones)
JOIN tratamiento tr ON tr.nombre = t.tratamiento;

-- 7. Reprogramación: la cita de mañana de María Gómez estaba para hoy a las 11:00
INSERT INTO cita_reprogramacion (id_cita, fecha_anterior, hora_anterior, fecha_nueva, hora_nueva, motivo, reprogramado_por)
VALUES ('de000000-0000-4000-8000-00000000c302', :'hoy', '11:00', :'manana', '10:00',
        'Cambio de turno en el trabajo de la paciente', 'de000000-0000-4000-8000-000000000003');

-- 8. Historial clínico
INSERT INTO historial_clinico (id_cliente, fecha, descripcion) VALUES
    ('de000000-0000-4000-8000-0000000000c1', :'ayer'::date + time '09:30', 'Paciente sin alergias conocidas. Se realizó limpieza; control en una semana.'),
    ('de000000-0000-4000-8000-0000000000c3', :'antes_fecha'::date + :'antes_hora'::time + interval '30 minutes',
     'Caries profunda en pieza 36, restaurada con resina.');

-- 9. Bloqueo: el Dr. Castillo no atiende pasado mañana por la mañana
INSERT INTO bloqueo_doctor (id_doctor, inicio, fin, motivo, creado_por)
VALUES ('de000000-0000-4000-8000-0000000000d2', :'pasado_manana'::date + time '08:00', :'pasado_manana'::date + time '13:00',
        'Congreso de ortodoncia', 'de000000-0000-4000-8000-000000000003');

-- 10. Inasistencias (HU-25). Luis Pérez: 3 en el pasado, levantamiento hace 40 días y 2 nuevas
--     (la de hace 10 días y la de ayer), así se ve que el conteo empieza de cero. Carmen Díaz: 3, bloqueada.
INSERT INTO cita (id_cita, id_cliente, id_doctor, fecha, hora, estado, notas, motivo_estado) VALUES
    ('de000000-0000-4000-8000-00000000c401', 'de000000-0000-4000-8000-0000000000c2', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy'::date - 60, '09:00', 'NO_ASISTIO', 'Limpieza dental', 'El paciente no se presentó'),
    ('de000000-0000-4000-8000-00000000c402', 'de000000-0000-4000-8000-0000000000c2', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy'::date - 55, '09:00', 'NO_ASISTIO', 'Limpieza dental', 'El paciente no se presentó'),
    ('de000000-0000-4000-8000-00000000c403', 'de000000-0000-4000-8000-0000000000c2', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy'::date - 50, '09:00', 'NO_ASISTIO', 'Limpieza dental', 'No contestó las llamadas'),
    ('de000000-0000-4000-8000-00000000c404', 'de000000-0000-4000-8000-0000000000c2', 'de000000-0000-4000-8000-0000000000d2',
     :'hoy'::date - 10, '10:00', 'NO_ASISTIO', 'Evaluación para brackets', 'Avisó tarde que no llegaba'),
    ('de000000-0000-4000-8000-00000000c501', 'de000000-0000-4000-8000-0000000000c5', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy'::date - 21, '11:00', 'NO_ASISTIO', 'Dolor de encías', 'La paciente no se presentó'),
    ('de000000-0000-4000-8000-00000000c502', 'de000000-0000-4000-8000-0000000000c5', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy'::date - 14, '11:00', 'NO_ASISTIO', 'Dolor de encías', 'La paciente no se presentó'),
    ('de000000-0000-4000-8000-00000000c503', 'de000000-0000-4000-8000-0000000000c5', 'de000000-0000-4000-8000-0000000000d2',
     :'hoy'::date - 7, '11:00', 'NO_ASISTIO', 'Evaluación de mordida', 'La paciente no se presentó'),
    ('de000000-0000-4000-8000-00000000c504', 'de000000-0000-4000-8000-0000000000c5', 'de000000-0000-4000-8000-0000000000d2',
     :'hoy'::date - 3, '14:00', 'CANCELADA', 'Evaluación de mordida', 'Cambio de trabajo');

INSERT INTO levantamiento_inasistencia (id_cliente, motivo, inasistencias, levantado_por, fecha) VALUES
    ('de000000-0000-4000-8000-0000000000c2', 'Se comprometió a avisar con anticipación', 3,
     'de000000-0000-4000-8000-000000000003', :'hoy'::date - 40 + time '10:00');

-- 11. Planes de tratamiento (HU-26)
INSERT INTO plan_tratamiento (id_plan, id_cliente, id_doctor, id_tratamiento, total_sesiones, costo_total, estado, fecha_creacion)
SELECT p.id_plan::uuid, p.id_cliente::uuid, p.id_doctor::uuid,
       (SELECT id_tratamiento FROM tratamiento WHERE nombre = p.tratamiento), p.sesiones, p.costo, 'ACTIVO', p.creado
FROM (VALUES
    ('de000000-0000-4000-8000-0000000000e1', 'de000000-0000-4000-8000-0000000000c1', 'de000000-0000-4000-8000-0000000000d1',
     'Endodoncia', 3, 4500.00, :'hoy'::date - 15 + time '10:00'),
    ('de000000-0000-4000-8000-0000000000e2', 'de000000-0000-4000-8000-0000000000c3', 'de000000-0000-4000-8000-0000000000d2',
     'Control de ortodoncia', 6, 1800.00, :'hoy'::date - 30 + time '11:00')
) AS p (id_plan, id_cliente, id_doctor, tratamiento, sesiones, costo, creado);

INSERT INTO cita (id_cita, id_cliente, id_doctor, fecha, hora, estado, notas, motivo_estado, id_plan) VALUES
    -- Ana López, endodoncia: 2 atendidas y la tercera pasado mañana
    ('de000000-0000-4000-8000-00000000c601', 'de000000-0000-4000-8000-0000000000c1', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy'::date - 14, '09:00', 'ATENDIDA', 'Sesion 1 de 3: Endodoncia', NULL, 'de000000-0000-4000-8000-0000000000e1'),
    ('de000000-0000-4000-8000-00000000c602', 'de000000-0000-4000-8000-0000000000c1', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy'::date - 7, '09:00', 'ATENDIDA', 'Sesion 2 de 3: Endodoncia', NULL, 'de000000-0000-4000-8000-0000000000e1'),
    ('de000000-0000-4000-8000-00000000c603', 'de000000-0000-4000-8000-0000000000c1', 'de000000-0000-4000-8000-0000000000d1',
     :'pasado_manana', '10:00', 'AGENDADA', 'Sesion 3 de 3: Endodoncia', NULL, 'de000000-0000-4000-8000-0000000000e1'),
    -- María Gómez, ortodoncia: 2 atendidas y 1 cancelada (su cita de mañana se vincula abajo)
    ('de000000-0000-4000-8000-00000000c701', 'de000000-0000-4000-8000-0000000000c3', 'de000000-0000-4000-8000-0000000000d2',
     :'hoy'::date - 28, '10:00', 'ATENDIDA', 'Sesion 1 de 6: Control de ortodoncia', NULL, 'de000000-0000-4000-8000-0000000000e2'),
    ('de000000-0000-4000-8000-00000000c702', 'de000000-0000-4000-8000-0000000000c3', 'de000000-0000-4000-8000-0000000000d2',
     :'hoy'::date - 21, '10:00', 'CANCELADA', 'Sesion 2 de 6: Control de ortodoncia', 'La paciente estaba enferma',
     'de000000-0000-4000-8000-0000000000e2'),
    ('de000000-0000-4000-8000-00000000c703', 'de000000-0000-4000-8000-0000000000c3', 'de000000-0000-4000-8000-0000000000d2',
     :'hoy'::date - 14, '10:00', 'ATENDIDA', 'Sesion 2 de 6: Control de ortodoncia', NULL, 'de000000-0000-4000-8000-0000000000e2');

UPDATE cita SET id_plan = 'de000000-0000-4000-8000-0000000000e2'
WHERE id_cita = 'de000000-0000-4000-8000-00000000c302';

INSERT INTO abono_plan (id_plan, monto, fecha, registrado_por) VALUES
    ('de000000-0000-4000-8000-0000000000e1', 1500.00, :'hoy'::date - 14 + time '09:45', 'de000000-0000-4000-8000-000000000003'),
    ('de000000-0000-4000-8000-0000000000e1', 1000.00, :'hoy'::date - 7 + time '09:50', 'de000000-0000-4000-8000-000000000003'),
    ('de000000-0000-4000-8000-0000000000e2',  600.00, :'hoy'::date - 28 + time '10:40', 'de000000-0000-4000-8000-000000000003');

-- 12. Más citas atendidas en las últimas semanas para el reporte de ingresos
INSERT INTO cita (id_cita, id_cliente, id_doctor, fecha, hora, estado, notas) VALUES
    ('de000000-0000-4000-8000-00000000c801', 'de000000-0000-4000-8000-0000000000c2', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy'::date - 5, '08:00', 'ATENDIDA', 'Muela con movilidad'),
    ('de000000-0000-4000-8000-00000000c802', 'de000000-0000-4000-8000-0000000000c3', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy'::date - 4, '15:00', 'ATENDIDA', 'Blanqueamiento'),
    ('de000000-0000-4000-8000-00000000c803', 'de000000-0000-4000-8000-0000000000c5', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy'::date - 3, '09:00', 'ATENDIDA', 'Limpieza dental');

-- Tratamientos de las sesiones de los planes y de las citas anteriores (algunos con costo ajustado)
INSERT INTO cita_tratamiento (id_cita, id_tratamiento, costo, observaciones, fecha_registro)
SELECT t.id_cita::uuid, tr.id_tratamiento, coalesce(t.costo, tr.costo), t.observaciones, c.fecha + c.hora + interval '30 minutes'
FROM (VALUES
    ('de000000-0000-4000-8000-00000000c601', 'Endodoncia',            1500.00,       'Apertura y limpieza de conductos, pieza 46'),
    ('de000000-0000-4000-8000-00000000c602', 'Endodoncia',            1500.00,       'Obturación de conductos, pieza 46'),
    ('de000000-0000-4000-8000-00000000c701', 'Control de ortodoncia', NULL::numeric, 'Colocación de arco superior'),
    ('de000000-0000-4000-8000-00000000c703', 'Control de ortodoncia', NULL::numeric, 'Cambio de ligas'),
    ('de000000-0000-4000-8000-00000000c801', 'Extracción simple',     NULL::numeric, 'Pieza 18'),
    ('de000000-0000-4000-8000-00000000c802', 'Blanqueamiento dental', 1350.00,       'Descuento por paciente frecuente'),
    ('de000000-0000-4000-8000-00000000c803', 'Limpieza dental',       NULL::numeric, NULL)
) AS t (id_cita, tratamiento, costo, observaciones)
JOIN tratamiento tr ON tr.nombre = t.tratamiento
JOIN cita c ON c.id_cita = t.id_cita::uuid;

COMMIT;

\echo 'Datos de demo cargados. Usuarios (todos con la contraseña indicada en clave_demo):'
SELECT u.email, r.nombre_rol AS rol, u.nombre || ' ' || u.apellido AS nombre
FROM usuario u JOIN rol r USING (id_rol)
WHERE u.id_usuario::text LIKE 'de000000-%'
ORDER BY r.nombre_rol, u.apellido;

\echo 'Citas en curso para la demo (el doctor ya las puede marcar):'
SELECT c.fecha, c.hora, ud.nombre || ' ' || ud.apellido AS doctor, up.nombre || ' ' || up.apellido AS paciente,
       c.notas
FROM cita c
JOIN doctor d USING (id_doctor)
JOIN usuario ud ON ud.id_usuario = d.id_usuario
JOIN cliente cl USING (id_cliente)
JOIN usuario up ON up.id_usuario = cl.id_usuario
WHERE c.id_cita IN ('de000000-0000-4000-8000-00000000c202', 'de000000-0000-4000-8000-00000000c203')
ORDER BY doctor DESC;

\echo 'Inasistencias desde el último levantamiento (con 3 o más el paciente no puede agendar):'
SELECT u.nombre || ' ' || u.apellido AS paciente,
       count(c.id_cita) FILTER (WHERE c.estado = 'NO_ASISTIO'
           AND (c.fecha + c.hora) > coalesce(l.ultimo, '-infinity')) AS inasistencias
FROM cliente cl
JOIN usuario u USING (id_usuario)
LEFT JOIN cita c ON c.id_cliente = cl.id_cliente
LEFT JOIN (SELECT id_cliente, max(fecha) AS ultimo FROM levantamiento_inasistencia GROUP BY id_cliente) l
       ON l.id_cliente = cl.id_cliente
WHERE cl.id_cliente::text LIKE 'de000000-%'
GROUP BY u.nombre, u.apellido
ORDER BY inasistencias DESC, u.apellido;

\echo 'Planes de tratamiento:'
SELECT u.nombre || ' ' || u.apellido AS paciente, t.nombre AS tratamiento, p.total_sesiones,
       (SELECT count(*) FROM cita c WHERE c.id_plan = p.id_plan AND c.estado = 'ATENDIDA') AS atendidas,
       p.costo_total,
       p.costo_total - coalesce((SELECT sum(monto) FROM abono_plan a WHERE a.id_plan = p.id_plan), 0) AS saldo
FROM plan_tratamiento p
JOIN cliente cl USING (id_cliente)
JOIN usuario u USING (id_usuario)
JOIN tratamiento t USING (id_tratamiento)
WHERE p.id_plan::text LIKE 'de000000-%'
ORDER BY u.apellido;
