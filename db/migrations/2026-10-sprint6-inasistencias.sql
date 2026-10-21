-- Migracion Sprint 6: levantamientos de la restriccion por inasistencias (HU-25).
-- Se aplica sobre una base que ya existe (EC2) sin borrar datos, despues de 2026-10-sprint5.sql.
-- Se puede correr varias veces seguidas sin error.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f db/migrations/2026-10-sprint6-inasistencias.sql
--
-- NUNCA correr db/clinica_dental_schema.sql contra EC2: empieza con DROP TABLE.

BEGIN;

-- Las inasistencias (citas NO_ASISTIO) se cuentan desde el ultimo levantamiento del paciente
CREATE TABLE IF NOT EXISTS levantamiento_inasistencia (
    id_levantamiento  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_cliente        UUID NOT NULL REFERENCES cliente(id_cliente) ON DELETE CASCADE,
    motivo            VARCHAR(200) NOT NULL,
    inasistencias     INTEGER NOT NULL CHECK (inasistencias >= 0),
    levantado_por     UUID REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    fecha             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_levantamiento_cliente ON levantamiento_inasistencia (id_cliente, fecha);

COMMIT;
