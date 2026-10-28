-- Migracion Sprint 6: planes de tratamiento con sesiones (citas vinculadas) y abonos (HU-26).
-- Se aplica sobre una base que ya existe (EC2) sin borrar datos, despues de 2026-10-sprint5.sql.
-- Se puede correr varias veces seguidas sin error.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f db/migrations/2026-10-sprint6-planes.sql
--
-- NUNCA correr db/clinica_dental_schema.sql contra EC2: empieza con DROP TABLE.

BEGIN;

-- 1. Plan de tratamiento de varias sesiones
CREATE TABLE IF NOT EXISTS plan_tratamiento (
    id_plan          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_cliente       UUID NOT NULL REFERENCES cliente(id_cliente) ON DELETE CASCADE,
    id_doctor        UUID NOT NULL REFERENCES doctor(id_doctor) ON DELETE CASCADE,
    id_tratamiento   UUID NOT NULL REFERENCES tratamiento(id_tratamiento),
    total_sesiones   INTEGER NOT NULL CHECK (total_sesiones > 0),
    costo_total      NUMERIC(10,2) NOT NULL CHECK (costo_total >= 0),
    estado           VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN
                        ('ACTIVO','COMPLETADO','CANCELADO')),
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_plan_cliente ON plan_tratamiento (id_cliente);
CREATE INDEX IF NOT EXISTS idx_plan_doctor ON plan_tratamiento (id_doctor);

-- 2. Cita vinculada a un plan como sesion; NULL si es una cita suelta
ALTER TABLE cita ADD COLUMN IF NOT EXISTS id_plan UUID
    REFERENCES plan_tratamiento(id_plan) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_cita_plan ON cita (id_plan);

-- 3. Abonos al plan; el saldo es costo_total menos la suma de abonos
CREATE TABLE IF NOT EXISTS abono_plan (
    id_abono        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_plan         UUID NOT NULL REFERENCES plan_tratamiento(id_plan) ON DELETE CASCADE,
    monto           NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    registrado_por  UUID REFERENCES usuario(id_usuario) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_abono_plan ON abono_plan (id_plan);

COMMIT;
