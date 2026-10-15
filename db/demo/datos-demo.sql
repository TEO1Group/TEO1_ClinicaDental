-- Datos de demo de la Clínica Dental (Sprint 5).
--
-- Crea: 2 doctores con horarios, 1 secretaria, 4 pacientes (uno en lista negra), citas de ayer, hoy y
-- mañana en todos los estados, tratamientos registrados, una reprogramación, historial clínico y un
-- bloqueo de doctor. Las fechas se calculan al correr el script (zona America/Guatemala).
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

\set ON_ERROR_STOP 1

\if :{?clave_demo}
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

BEGIN;

-- 1. Limpiar la demo anterior. Al borrar los usuarios se borran en cascada sus perfiles, citas,
--    tratamientos de cita, reprogramaciones, historial, horarios y bloqueos.
DELETE FROM usuario WHERE id_usuario IN (
    'de000000-0000-4000-8000-000000000001', 'de000000-0000-4000-8000-000000000002',
    'de000000-0000-4000-8000-000000000003', 'de000000-0000-4000-8000-000000000011',
    'de000000-0000-4000-8000-000000000012', 'de000000-0000-4000-8000-000000000013',
    'de000000-0000-4000-8000-000000000014'
);

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
    ('de000000-0000-4000-8000-000000000014', 'Jorge',  'Ruiz',     'jorge.ruiz@clinica-demo.test',     'CLIENTE',    '55520004')
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
     'Tres inasistencias seguidas sin avisar');

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
    -- Hoy
    ('de000000-0000-4000-8000-00000000c201', 'de000000-0000-4000-8000-0000000000c3', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy', '08:00', 'ATENDIDA', 'Caries en pieza 36', NULL),
    ('de000000-0000-4000-8000-00000000c202', 'de000000-0000-4000-8000-0000000000c1', 'de000000-0000-4000-8000-0000000000d1',
     :'hoy', '15:00', 'AGENDADA', 'Control después de la limpieza', NULL),
    ('de000000-0000-4000-8000-00000000c203', 'de000000-0000-4000-8000-0000000000c2', 'de000000-0000-4000-8000-0000000000d2',
     :'hoy', '16:30', 'AGENDADA', 'Evaluación para brackets', NULL),
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
    ('de000000-0000-4000-8000-0000000000c3', :'hoy'::date + time '08:30', 'Caries profunda en pieza 36, restaurada con resina.');

-- 9. Bloqueo: el Dr. Castillo no atiende pasado mañana por la mañana
INSERT INTO bloqueo_doctor (id_doctor, inicio, fin, motivo, creado_por)
VALUES ('de000000-0000-4000-8000-0000000000d2', :'pasado_manana'::date + time '08:00', :'pasado_manana'::date + time '13:00',
        'Congreso de ortodoncia', 'de000000-0000-4000-8000-000000000003');

COMMIT;

\echo 'Datos de demo cargados. Usuarios (todos con la contraseña indicada en clave_demo):'
SELECT u.email, r.nombre_rol AS rol, u.nombre || ' ' || u.apellido AS nombre
FROM usuario u JOIN rol r USING (id_rol)
WHERE u.id_usuario::text LIKE 'de000000-%'
ORDER BY r.nombre_rol, u.apellido;
