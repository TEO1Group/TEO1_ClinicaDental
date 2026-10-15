-- Migracion Sprint 5: motivo de estado, bloqueos de doctor, tratamientos por cita y reprogramaciones.
-- Se aplica sobre una base que ya existe (EC2) sin borrar datos.
-- Se puede correr varias veces seguidas sin error.
--
-- Uso:
--   psql -h <host> -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f db/migrations/2026-10-sprint5.sql
--
-- NUNCA correr db/clinica_dental_schema.sql contra EC2: empieza con DROP TABLE.

BEGIN;

-- 1. Motivo del ultimo cambio de estado de la cita (HU-18)
ALTER TABLE cita ADD COLUMN IF NOT EXISTS motivo_estado VARCHAR(200);

-- 2. Periodos en los que un doctor no atiende (HU-20)
CREATE TABLE IF NOT EXISTS bloqueo_doctor (
    id_bloqueo      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_doctor       UUID NOT NULL REFERENCES doctor(id_doctor) ON DELETE CASCADE,
    inicio          TIMESTAMP NOT NULL,
    fin             TIMESTAMP NOT NULL,
    motivo          VARCHAR(200) NOT NULL,
    creado_por      UUID REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    fecha_creacion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (fin > inicio)
);
CREATE INDEX IF NOT EXISTS idx_bloqueo_doctor ON bloqueo_doctor (id_doctor, inicio);

-- 3. Tratamientos por cita (HU-21): id propio, costo cobrado, observaciones y fecha
ALTER TABLE cita_tratamiento ADD COLUMN IF NOT EXISTS id_cita_tratamiento UUID DEFAULT gen_random_uuid();
UPDATE cita_tratamiento SET id_cita_tratamiento = gen_random_uuid() WHERE id_cita_tratamiento IS NULL;
ALTER TABLE cita_tratamiento ALTER COLUMN id_cita_tratamiento SET NOT NULL;

-- La llave primaria pasa de (id_cita, id_tratamiento) a id_cita_tratamiento, solo si todavia no se cambio
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_index i
        JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY (i.indkey)
        WHERE i.indrelid = 'cita_tratamiento'::regclass
          AND i.indisprimary
          AND a.attname = 'id_cita_tratamiento'
    ) THEN
        ALTER TABLE cita_tratamiento DROP CONSTRAINT IF EXISTS cita_tratamiento_pkey;
        ALTER TABLE cita_tratamiento ADD CONSTRAINT cita_tratamiento_pkey PRIMARY KEY (id_cita_tratamiento);
    END IF;
END $$;

-- Si ya habia filas, toman el costo del catalogo
ALTER TABLE cita_tratamiento ADD COLUMN IF NOT EXISTS costo NUMERIC(10,2);
UPDATE cita_tratamiento ct
SET costo = t.costo
FROM tratamiento t
WHERE ct.id_tratamiento = t.id_tratamiento
  AND ct.costo IS NULL;
ALTER TABLE cita_tratamiento ALTER COLUMN costo SET NOT NULL;
ALTER TABLE cita_tratamiento DROP CONSTRAINT IF EXISTS cita_tratamiento_costo_check;
ALTER TABLE cita_tratamiento ADD CONSTRAINT cita_tratamiento_costo_check CHECK (costo >= 0);

ALTER TABLE cita_tratamiento ADD COLUMN IF NOT EXISTS observaciones TEXT;
ALTER TABLE cita_tratamiento ADD COLUMN IF NOT EXISTS fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_cita_tratamiento_cita ON cita_tratamiento (id_cita);

-- 4. Reprogramaciones (HU-19): motivo obligatorio y quien la hizo
UPDATE cita_reprogramacion SET motivo = 'Sin motivo registrado' WHERE motivo IS NULL;
ALTER TABLE cita_reprogramacion ALTER COLUMN motivo SET NOT NULL;
ALTER TABLE cita_reprogramacion ADD COLUMN IF NOT EXISTS reprogramado_por UUID
    REFERENCES usuario(id_usuario) ON DELETE SET NULL;

-- 5. Catalogo inicial de tratamientos, solo si la tabla esta vacia
CREATE UNIQUE INDEX IF NOT EXISTS uq_tratamiento_nombre ON tratamiento (nombre);
INSERT INTO tratamiento (nombre, descripcion, duracion_minutos, costo)
SELECT * FROM (VALUES
    ('Consulta y diagnóstico', 'Evaluación general y plan de tratamiento', 30, 150.00),
    ('Limpieza dental', 'Profilaxis: remoción de placa y sarro', 45, 300.00),
    ('Aplicación de flúor', 'Aplicación tópica de flúor', 20, 150.00),
    ('Sellantes de fosas y fisuras', 'Sellado preventivo por pieza', 30, 200.00),
    ('Radiografía periapical', 'Radiografía de una pieza dental', 15, 100.00),
    ('Resina (empaste)', 'Restauración con resina por pieza', 45, 400.00),
    ('Extracción simple', 'Extracción de una pieza dental', 30, 350.00),
    ('Extracción de muela del juicio', 'Extracción quirúrgica de tercer molar', 60, 1200.00),
    ('Endodoncia', 'Tratamiento de conducto', 90, 1800.00),
    ('Blanqueamiento dental', 'Blanqueamiento en consultorio', 60, 1500.00)
) AS catalogo (nombre, descripcion, duracion_minutos, costo)
WHERE NOT EXISTS (SELECT 1 FROM tratamiento);

COMMIT;
