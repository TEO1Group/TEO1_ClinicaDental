

CREATE EXTENSION IF NOT EXISTS "pgcrypto"; 
DROP TABLE IF EXISTS pago CASCADE;
DROP TABLE IF EXISTS valoracion CASCADE;
DROP TABLE IF EXISTS bloqueo_doctor CASCADE;
DROP TABLE IF EXISTS cita_reprogramacion CASCADE;
DROP TABLE IF EXISTS cita_tratamiento CASCADE;
DROP TABLE IF EXISTS tratamiento_medicamento CASCADE;
DROP TABLE IF EXISTS cita CASCADE;
DROP TABLE IF EXISTS medicamento CASCADE;
DROP TABLE IF EXISTS tratamiento CASCADE;
DROP TABLE IF EXISTS historial_clinico CASCADE;
DROP TABLE IF EXISTS horario CASCADE;
DROP TABLE IF EXISTS secretaria CASCADE;
DROP TABLE IF EXISTS doctor CASCADE;
DROP TABLE IF EXISTS cliente CASCADE;
DROP TABLE IF EXISTS usuario CASCADE;
DROP TABLE IF EXISTS rol CASCADE;

CREATE TABLE rol (
    id_rol      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_rol  VARCHAR(20) NOT NULL UNIQUE CHECK (nombre_rol IN ('ADMIN','DOCTOR','SECRETARIA','CLIENTE')),
    descripcion VARCHAR(200)
);

INSERT INTO rol (nombre_rol, descripcion) VALUES
    ('ADMIN', 'Administrador del sistema'),
    ('DOCTOR', 'Doctor que atiende citas'),
    ('SECRETARIA', 'Gestiona horarios, clientes y lista negra'),
    ('CLIENTE', 'Paciente de la clínica');

CREATE TABLE usuario (
    id_usuario      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre          VARCHAR(80)  NOT NULL,
    apellido        VARCHAR(80)  NOT NULL,
    email           VARCHAR(120) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    id_rol          UUID NOT NULL REFERENCES rol(id_rol),
    telefono        VARCHAR(20),
    estado          VARCHAR(15)  NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','INACTIVO')),
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cliente (
    id_cliente          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario          UUID NOT NULL UNIQUE REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    dpi                 VARCHAR(13) NOT NULL UNIQUE,
    direccion           VARCHAR(200),
    fecha_nacimiento    DATE,
    en_lista_negra      BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_lista_negra  VARCHAR(200)
);

CREATE TABLE historial_clinico (
    id_historial  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_cliente    UUID NOT NULL REFERENCES cliente(id_cliente) ON DELETE CASCADE,
    fecha         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descripcion   TEXT NOT NULL
);

CREATE TABLE doctor (
    id_doctor              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario             UUID NOT NULL UNIQUE REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    especialidad           VARCHAR(100) NOT NULL,
    numero_colegiado       VARCHAR(30),
    numero_clinica         VARCHAR(20),
    calificacion_promedio  NUMERIC(3,2) DEFAULT 0
);

CREATE TABLE secretaria (
    id_secretaria  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario     UUID NOT NULL UNIQUE REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    turno          VARCHAR(15) CHECK (turno IN ('MANANA','TARDE','NOCHE')),
    area           VARCHAR(30) DEFAULT 'RECEPCION'
);

CREATE TABLE horario (
    id_horario    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_doctor     UUID NOT NULL REFERENCES doctor(id_doctor) ON DELETE CASCADE,
    dia_semana    VARCHAR(15) NOT NULL CHECK (dia_semana IN
                    ('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO')),
    hora_inicio   TIME NOT NULL,
    hora_fin      TIME NOT NULL,
    CHECK (hora_fin > hora_inicio)
);

CREATE TABLE tratamiento (
    id_tratamiento   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre           VARCHAR(100) NOT NULL,
    descripcion      TEXT,
    duracion_minutos INTEGER NOT NULL CHECK (duracion_minutos > 0),
    costo            NUMERIC(10,2) NOT NULL CHECK (costo >= 0)
);

-- Catalogo inicial de tratamientos (el costo es el precio sugerido; se puede ajustar al registrarlo en la cita)
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

CREATE TABLE medicamento (
    id_medicamento    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre            VARCHAR(100) NOT NULL,
    descripcion       TEXT,
    stock             INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    unidad_medida     VARCHAR(30),
    fecha_vencimiento DATE
);

CREATE TABLE tratamiento_medicamento (
    id_tratamiento  UUID NOT NULL REFERENCES tratamiento(id_tratamiento) ON DELETE CASCADE,
    id_medicamento  UUID NOT NULL REFERENCES medicamento(id_medicamento) ON DELETE CASCADE,
    cantidad        INTEGER NOT NULL CHECK (cantidad > 0),
    PRIMARY KEY (id_tratamiento, id_medicamento)
);

CREATE TABLE cita (
    id_cita     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_cliente  UUID NOT NULL REFERENCES cliente(id_cliente) ON DELETE CASCADE,
    id_doctor   UUID NOT NULL REFERENCES doctor(id_doctor) ON DELETE CASCADE,
    fecha       DATE NOT NULL,
    hora        TIME NOT NULL,
    estado      VARCHAR(15) NOT NULL DEFAULT 'AGENDADA' CHECK (estado IN
                    ('AGENDADA','ATENDIDA','CANCELADA','NO_ASISTIO')),
    notas       TEXT,
    recordatorio_enviado BOOLEAN NOT NULL DEFAULT FALSE,
    -- Motivo del ultimo cambio de estado (obligatorio al cancelar o marcar NO_ASISTIO)
    motivo_estado VARCHAR(200)
);

-- Evita que un mismo doctor tenga dos citas activas en la misma fecha y hora.
-- Las canceladas no cuentan, asi la hora se puede volver a agendar.
CREATE UNIQUE INDEX uq_cita_doctor_slot ON cita (id_doctor, fecha, hora)
    WHERE estado <> 'CANCELADA';

-- Tratamientos realizados en una cita. Una cita puede tener el mismo tratamiento mas de una vez
-- (por ejemplo, dos extracciones), por eso tiene su propio id.
CREATE TABLE cita_tratamiento (
    id_cita_tratamiento  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_cita              UUID NOT NULL REFERENCES cita(id_cita) ON DELETE CASCADE,
    id_tratamiento       UUID NOT NULL REFERENCES tratamiento(id_tratamiento),
    costo                NUMERIC(10,2) NOT NULL CHECK (costo >= 0),
    observaciones        TEXT,
    fecha_registro       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cita_reprogramacion (
    id_reprogramacion  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_cita            UUID NOT NULL REFERENCES cita(id_cita) ON DELETE CASCADE,
    fecha_anterior     DATE NOT NULL,
    hora_anterior      TIME NOT NULL,
    fecha_nueva        DATE NOT NULL,
    hora_nueva         TIME NOT NULL,
    motivo             VARCHAR(200) NOT NULL,
    fecha_cambio       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reprogramado_por   UUID REFERENCES usuario(id_usuario) ON DELETE SET NULL
);

-- Periodos en los que un doctor no atiende: no se puede agendar ni reprogramar dentro de ellos
CREATE TABLE bloqueo_doctor (
    id_bloqueo      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_doctor       UUID NOT NULL REFERENCES doctor(id_doctor) ON DELETE CASCADE,
    inicio          TIMESTAMP NOT NULL,
    fin             TIMESTAMP NOT NULL,
    motivo          VARCHAR(200) NOT NULL,
    creado_por      UUID REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    fecha_creacion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (fin > inicio)
);


CREATE TABLE valoracion (
    id_valoracion  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_cita        UUID NOT NULL UNIQUE REFERENCES cita(id_cita) ON DELETE CASCADE,
    puntuacion     SMALLINT NOT NULL CHECK (puntuacion BETWEEN 1 AND 5),
    comentario     TEXT,
    fecha          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pago (
    id_pago     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_cita     UUID NOT NULL REFERENCES cita(id_cita) ON DELETE CASCADE,
    monto       NUMERIC(10,2) NOT NULL CHECK (monto >= 0),
    fecha_pago  TIMESTAMP,
    estado      VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN
                    ('PAGADO','PENDIENTE','DEUDA'))
);


CREATE INDEX idx_cita_cliente     ON cita(id_cliente);
CREATE INDEX idx_cita_doctor      ON cita(id_doctor);
CREATE INDEX idx_cita_fecha       ON cita(fecha);
CREATE INDEX idx_usuario_rol      ON usuario(id_rol);
CREATE INDEX idx_horario_doctor   ON horario(id_doctor);
CREATE INDEX idx_reprog_cita      ON cita_reprogramacion(id_cita);
CREATE INDEX idx_historial_cliente ON historial_clinico(id_cliente);
CREATE INDEX idx_cita_tratamiento_cita ON cita_tratamiento(id_cita);
CREATE INDEX idx_bloqueo_doctor   ON bloqueo_doctor(id_doctor, inicio);
CREATE UNIQUE INDEX uq_tratamiento_nombre ON tratamiento(nombre);
