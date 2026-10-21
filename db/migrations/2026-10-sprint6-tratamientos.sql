-- Apply to an existing database after Sprint 5; never run the full schema on production.
-- Repeatable: preserve existing names, prices, active flags and historical charges.
BEGIN;
ALTER TABLE tratamiento ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
-- Proposed additions: names, durations and prices require team approval before merge.
INSERT INTO tratamiento (nombre, descripcion, duracion_minutos, costo)
VALUES
    ('Corona de zirconio', 'Restauración fija por pieza', 60, 2500.00),
    ('Corona metal porcelana', 'Corona fija por pieza', 60, 2000.00),
    ('Puente dental', 'Reposición fija de una pieza ausente', 90, 3500.00),
    ('Prótesis parcial removible', 'Reposición removible de piezas', 60, 2800.00),
    ('Prótesis total', 'Prótesis completa por arcada', 90, 4500.00),
    ('Incrustación dental', 'Restauración indirecta por pieza', 60, 1600.00),
    ('Carilla de resina', 'Restauración estética anterior por pieza', 45, 900.00),
    ('Carilla de porcelana', 'Restauración estética cerámica por pieza', 60, 2500.00),
    ('Raspado y alisado radicular', 'Tratamiento periodontal por cuadrante', 60, 800.00),
    ('Control de ortodoncia', 'Revisión y ajuste periódico', 30, 300.00)
ON CONFLICT (nombre) DO NOTHING;
COMMIT;
