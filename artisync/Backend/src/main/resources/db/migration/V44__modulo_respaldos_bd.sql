-- ==============================================================================
-- ARTISYNC — Módulo de Respaldos de Base de Datos (REQ-NF-024)
-- ==============================================================================

-- 1. Tabla de registro de respaldos (archivos .dump/.sql.gz almacenados localmente)
CREATE TABLE respaldos_bd (
    id_respaldo BIGSERIAL PRIMARY KEY,
    nombre_archivo VARCHAR(255) NOT NULL UNIQUE,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('AUTOMATICO', 'MANUAL')),
    tamano_bytes BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'COMPLETADO' 
        CHECK (estado IN ('EN_PROGRESO', 'COMPLETADO', 'FALLIDO')),
    mensaje_error TEXT,
    creado_por VARCHAR(150),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP,
    hash_sha256 VARCHAR(64)
);

CREATE INDEX idx_respaldos_fecha ON respaldos_bd(fecha_creacion DESC);
CREATE INDEX idx_respaldos_tipo ON respaldos_bd(tipo);

-- 2. Tabla para almacenar la configuración de la política dinámica
CREATE TABLE respaldos_politica (
    id_politica INTEGER PRIMARY KEY CHECK (id_politica = 1),
    cron_expresion VARCHAR(100) NOT NULL DEFAULT '0 0 2 * * ?',
    retencion_dias INTEGER NOT NULL DEFAULT 7
);

-- Seed de la política inicial por defecto
INSERT INTO respaldos_politica (id_politica, cron_expresion, retencion_dias) VALUES (1, '0 0 2 * * ?', 7);

-- 3. Permisos explícitos para el usuario de la aplicación (en caso de que el esquema
-- ya esté inicializado antes de esta migración)
GRANT SELECT, INSERT, UPDATE, DELETE ON respaldos_bd TO artisync_app;
GRANT USAGE, SELECT ON SEQUENCE respaldos_bd_id_respaldo_seq TO artisync_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON respaldos_politica TO artisync_app;

-- 4. Creación de permisos atómicos y asignación al rol ADMIN
INSERT INTO permisos (nombre_permiso, modulo_aplicacion) VALUES 
('RESPALDO_VER', 'SISTEMA'),
('RESPALDO_CREAR', 'SISTEMA'),
('RESPALDO_ELIMINAR', 'SISTEMA'),
('RESPALDO_RESTAURAR', 'SISTEMA'),
('RESPALDO_CONFIGURAR', 'SISTEMA');

-- Asignar a ADMIN (asumiendo que ADMIN ya existe por V1)
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'ADMIN' AND p.nombre_permiso IN (
    'RESPALDO_VER', 'RESPALDO_CREAR', 'RESPALDO_ELIMINAR', 'RESPALDO_RESTAURAR', 'RESPALDO_CONFIGURAR'
);
