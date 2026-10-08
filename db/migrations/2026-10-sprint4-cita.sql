-- Migracion Sprint 4: tabla cita lista para el agendamiento.
-- Se aplica sobre una base que ya existe (EC2) sin borrar datos.
-- Se puede correr varias veces seguidas sin error.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f db/migrations/2026-10-sprint4-cita.sql
--
-- NUNCA correr db/clinica_dental_schema.sql contra EC2: empieza con DROP TABLE.

BEGIN;

-- 1. Estados: se quita el CHECK viejo antes de renombrar los datos
ALTER TABLE cita DROP CONSTRAINT IF EXISTS cita_estado_check;

UPDATE cita SET estado = 'ATENDIDA'   WHERE estado = 'COMPLETADA';
UPDATE cita SET estado = 'NO_ASISTIO' WHERE estado = 'INASISTENCIA';
-- APLAZADA desaparece: una cita aplazada sigue pendiente
UPDATE cita SET estado = 'AGENDADA'   WHERE estado = 'APLAZADA';

ALTER TABLE cita ADD CONSTRAINT cita_estado_check
    CHECK (estado IN ('AGENDADA','ATENDIDA','CANCELADA','NO_ASISTIO'));

-- 2. Unicidad: las citas canceladas ya no bloquean la hora
ALTER TABLE cita DROP CONSTRAINT IF EXISTS cita_id_doctor_fecha_hora_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_cita_doctor_slot ON cita (id_doctor, fecha, hora)
    WHERE estado <> 'CANCELADA';

-- 3. Marca para el correo de recordatorio (HU-15)
ALTER TABLE cita ADD COLUMN IF NOT EXISTS recordatorio_enviado BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;
