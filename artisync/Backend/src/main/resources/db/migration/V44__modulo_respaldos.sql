-- ==============================================================================
-- MIGRACIÓN V44: MÓDULO DE RESPALDOS DE BASE DE DATOS (REQ-NF-024)
-- ==============================================================================
-- Ver docs/despliegue/BACKUP.md para la política de respaldo/retención. Módulo
-- nuevo completo en un solo archivo (tablas + índices + grants + permisos),
-- mismo criterio que V15__modulo_auditoria.sql para "módulo nuevo desde cero".
--
-- Respaldo FULL: pg_dump -Fc contra el rol de solo lectura artisync_backup
-- (ver db/seed_privilegios.sh). Respaldo INCREMENTAL: exportación diferencial
-- por tabla vía COPY TO STDOUT (JDBC CopyManager), NO un pg_dump filtrado —
-- pg_dump no admite filtrar filas por WHERE. Ver PgDumpEjecutor /
-- IncrementalRespaldoExportador.

-- ------------------------------------------------------------------------------
-- 1. Tabla respaldo_programaciones (se crea primero: respaldos la referencia)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS respaldo_programaciones (
    id_programacion     BIGSERIAL PRIMARY KEY,
    nombre               VARCHAR(100) NOT NULL,
    tipo_respaldo         VARCHAR(20)  NOT NULL,
    expresion_cron        VARCHAR(100) NOT NULL,
    retencion_dias        INTEGER      NOT NULL,
    activo                BOOLEAN      NOT NULL DEFAULT true,
    proxima_ejecucion     TIMESTAMP    NOT NULL,
    ultima_ejecucion      TIMESTAMP,
    creado_por            VARCHAR(150) NOT NULL,
    fecha_creacion        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_programacion_tipo CHECK (tipo_respaldo IN ('FULL', 'INCREMENTAL')),
    CONSTRAINT ck_programacion_retencion CHECK (retencion_dias > 0)
);

-- El scheduler hace exactamente este WHERE cada 60s
-- (BackupScheduler.procesarProgramacionesPendientes).
CREATE INDEX IF NOT EXISTS idx_programaciones_pendientes
    ON respaldo_programaciones (proxima_ejecucion)
    WHERE activo = true;

-- ------------------------------------------------------------------------------
-- 2. Tabla respaldos
-- ------------------------------------------------------------------------------
-- Columnas de fecha en TIMESTAMP (no TIMESTAMPTZ): coherencia con
-- auditoria_eventos.fecha_evento (V15) y con el mapeo por defecto de
-- LocalDateTime que ddl-auto=validate comprueba estrictamente. actualizado_en
-- de respaldo_programaciones lo fija el servicio Java en cada mutación, no un
-- trigger de BD (a diferencia de usuarios/portafolios en V2, que sí usan
-- TIMESTAMPTZ + set_actualizado_en() -- inconsistencia ya existente en el
-- esquema; este módulo nuevo sigue el precedente más reciente de V15).
CREATE TABLE IF NOT EXISTS respaldos (
    id_respaldo               BIGSERIAL PRIMARY KEY,
    tipo_respaldo              VARCHAR(20)  NOT NULL,
    estado_respaldo            VARCHAR(20)  NOT NULL DEFAULT 'EN_PROGRESO',
    origen                     VARCHAR(20)  NOT NULL,
    id_programacion            BIGINT REFERENCES respaldo_programaciones(id_programacion) ON DELETE SET NULL,
    id_respaldo_full_base      BIGINT REFERENCES respaldos(id_respaldo),
    nombre_archivo             VARCHAR(255),
    ruta_archivo               VARCHAR(500),
    tamano_bytes               BIGINT,
    fecha_inicio               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin                  TIMESTAMP,
    duracion_ms                INTEGER,
    mensaje_error               VARCHAR(500),
    correo_solicitante          VARCHAR(150) NOT NULL,
    fecha_desde_incremental     TIMESTAMP,
    CONSTRAINT ck_respaldo_tipo CHECK (tipo_respaldo IN ('FULL', 'INCREMENTAL')),
    CONSTRAINT ck_respaldo_estado CHECK (estado_respaldo IN ('EN_PROGRESO', 'COMPLETADO', 'FALLIDO')),
    CONSTRAINT ck_respaldo_origen CHECK (origen IN ('MANUAL', 'PROGRAMADO')),
    -- Sostiene la lógica de retención/eliminación (RespaldoRetencionScheduler,
    -- RespaldoServicioImpl.eliminar): un FULL nunca depende de otro respaldo;
    -- un INCREMENTAL siempre declara de qué FULL depende.
    CONSTRAINT ck_respaldo_base_coherente CHECK (
        (tipo_respaldo = 'FULL' AND id_respaldo_full_base IS NULL) OR
        (tipo_respaldo = 'INCREMENTAL' AND id_respaldo_full_base IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_respaldos_fecha_inicio ON respaldos (fecha_inicio DESC);
CREATE INDEX IF NOT EXISTS idx_respaldos_tipo_estado ON respaldos (tipo_respaldo, estado_respaldo);

-- "¿Todavía hay incrementales COMPLETADO que dependen de esta FULL?" — la
-- consulta que RespaldoRetencionScheduler y la eliminación manual corren
-- antes de borrar un FULL.
CREATE INDEX IF NOT EXISTS idx_respaldos_full_base
    ON respaldos (id_respaldo_full_base)
    WHERE id_respaldo_full_base IS NOT NULL;

-- ------------------------------------------------------------------------------
-- 3. GRANTs
-- ------------------------------------------------------------------------------
-- artisync_app: CRUD normal de la app sobre las 2 tablas nuevas.
-- artisync_backup: rol nuevo de SOLO LECTURA (ver db/seed_privilegios.sh) que
-- usan PgDumpEjecutor/IncrementalRespaldoExportador para leer TODO el
-- esquema, incluida esta tabla de metadatos si algún día hiciera falta.
-- IF EXISTS: mismo patrón defensivo que V15 usa para artisync_app -- en una
-- máquina donde seed_privilegios.sh aún no creó el rol, un GRANT a un rol
-- inexistente abortaría toda la migración.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON respaldos, respaldo_programaciones TO artisync_app;
        GRANT USAGE, SELECT ON SEQUENCE respaldos_id_respaldo_seq TO artisync_app;
        GRANT USAGE, SELECT ON SEQUENCE respaldo_programaciones_id_programacion_seq TO artisync_app;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_backup') THEN
        GRANT SELECT ON ALL TABLES IN SCHEMA public TO artisync_backup;
        GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO artisync_backup;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO artisync_backup;
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO artisync_backup;
    END IF;
END
$$;

-- ------------------------------------------------------------------------------
-- 4. Permisos nuevos y asignación a ADMIN
-- ------------------------------------------------------------------------------
-- RESPALDO_DESCARGAR va separado de RESPALDO_VER a propósito: un dump FULL
-- contiene la base de datos entera (todo usuario, todo dato), más sensible
-- que el CSV de auditoría que ya separa AUDITORIA_VER/AUDITORIA_EXPORTAR.
INSERT INTO permisos (nombre_permiso, modulo_aplicacion)
VALUES
    ('RESPALDO_VER',       'SISTEMA'),
    ('RESPALDO_CREAR',     'SISTEMA'),
    ('RESPALDO_DESCARGAR', 'SISTEMA'),
    ('RESPALDO_ELIMINAR',  'SISTEMA'),
    ('RESPALDO_PROGRAMAR', 'SISTEMA')
ON CONFLICT (nombre_permiso) DO UPDATE SET modulo_aplicacion = EXCLUDED.modulo_aplicacion;

-- ADMIN: INSERT explícito. El cross-join de db/seed.sql que da todos los
-- permisos a ADMIN ya corrió antes de que Flyway aplicara esta migración
-- (mismo motivo documentado en V15).
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'ADMIN'
  AND p.nombre_permiso IN ('RESPALDO_VER', 'RESPALDO_CREAR', 'RESPALDO_DESCARGAR', 'RESPALDO_ELIMINAR', 'RESPALDO_PROGRAMAR')
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
