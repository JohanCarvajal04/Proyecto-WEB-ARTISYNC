-- ==============================================================================
-- MIGRACIÓN V12: MÓDULO DE AUDITORÍA TRANSVERSAL (REQ-NF-013)
-- ==============================================================================
--
-- El SRS declara REQ-NF-013 ("auditoría inmutable de transiciones de pedido y
-- transacciones; exportación CSV por el administrador") pero hasta ahora lo
-- único que existía con ese nombre era AuditControlador/AuditService, que solo
-- exporta un CSV de transacciones de un creador. No hay bitácora: los eventos
-- de login van a un log.info de SLF4J (no persistido) y
-- historial_estados_pedido es historial DE DOMINIO (sin actor, sin IP, se
-- borra en cascada con el pedido, y no registra los intentos FALLIDOS de
-- transición). Esta migración crea la tabla que sí cumple el requisito.
--
-- auditoria_eventos NO sustituye ni duplica historial_estados_pedido: son
-- cosas distintas y coexisten. El historial es de dominio (lo consume el
-- cliente en su pantalla de seguimiento); esta tabla es forense (inmutable,
-- sobrevive al borrado del pedido, incluye quién y desde qué IP, e incluye
-- los intentos fallidos que el historial jamás registra). El vínculo entre
-- ambas es entidad_afectada='pedidos' + id_entidad_afectada.
--
-- Idempotente a propósito, con el mismo motivo documentado en V10 (líneas
-- 19-22): en el arranque de Docker `db/seed.sql` se ejecuta como init-script
-- ANTES de que Flyway aplique V6..V12, así que los dos permisos nuevos deben
-- declararse a la vez aquí y en seed.sql, y la asignación a ADMIN debe ser
-- explícita: el cross-join de seed.sql que le da todos los permisos ya corrió
-- antes de que esta migración exista.

-- ------------------------------------------------------------------------------
-- 1. Tabla
-- ------------------------------------------------------------------------------
-- fecha_evento es TIMESTAMP (no TIMESTAMPTZ) por coherencia con el resto de
-- columnas fecha_* del esquema (p. ej. usuarios.fecha_registro de V1) y con el
-- mapeo por defecto de LocalDateTime que `spring.jpa.hibernate.ddl-auto=validate`
-- comprueba estrictamente contra el tipo SQL.
--
-- id_usuario_actor NO lleva FK a usuarios a propósito: un ON DELETE SET NULL
-- dispararía un UPDATE que el trigger de inmutabilidad de más abajo bloquearía,
-- haciendo imposible borrar un usuario. correo_actor va desnormalizado y es
-- NOT NULL porque siempre hay un valor que registrar: el correo tecleado en un
-- login fallido (aunque no exista como usuario), o 'sistema'/'sistema:paypal'
-- para tareas y webhooks sin actor humano.
CREATE TABLE IF NOT EXISTS auditoria_eventos (
    id_evento_auditoria BIGSERIAL PRIMARY KEY,
    fecha_evento        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario_actor    BIGINT,
    correo_actor        VARCHAR(150) NOT NULL,
    modulo_auditoria    VARCHAR(20)  NOT NULL,
    accion_auditoria    VARCHAR(60)  NOT NULL,
    resultado_evento    VARCHAR(10)  NOT NULL,
    entidad_afectada    VARCHAR(60),
    id_entidad_afectada BIGINT,
    detalle_cambio      JSONB,
    mensaje_error       VARCHAR(500),
    direccion_ip        VARCHAR(45),
    agente_usuario      VARCHAR(255),
    metodo_http         VARCHAR(10),
    ruta_solicitud      VARCHAR(255),
    duracion_ms         INTEGER,
    CONSTRAINT ck_auditoria_resultado
        CHECK (resultado_evento IN ('EXITO', 'FALLIDO', 'DENEGADO')),
    CONSTRAINT ck_auditoria_modulo
        CHECK (modulo_auditoria IN ('SEGURIDAD', 'SISTEMA', 'PORTAFOLIO', 'CATALOGO',
                                     'PEDIDOS', 'FINANZAS', 'COMUNICACION', 'SOCIAL'))
);

COMMENT ON COLUMN auditoria_eventos.detalle_cambio IS
    'JSONB sin indice GIN a proposito: hoy ninguna consulta filtra dentro del '
    'JSON y el coste de mantenerlo en cada INSERT no se justifica. Anadir un '
    'indice GIN aqui si en el futuro se necesita buscar por una clave interna.';

-- ------------------------------------------------------------------------------
-- 2. Índices — todos con la fecha al final para resolver WHERE y ORDER BY
--    fecha_evento DESC sin un sort adicional.
-- ------------------------------------------------------------------------------

-- Carga inicial de la pantalla, sin filtros: la tabla más "viva" del sistema
-- necesita este índice o cada página cero es un seq scan + sort completo.
CREATE INDEX IF NOT EXISTS idx_auditoria_fecha
    ON auditoria_eventos (fecha_evento DESC);

-- "Todo lo que hizo el usuario X" ordenado por fecha.
CREATE INDEX IF NOT EXISTS idx_auditoria_actor_fecha
    ON auditoria_eventos (id_usuario_actor, fecha_evento DESC);

-- Filtro por módulo (8 valores, baja selectividad, pero evita el sort).
CREATE INDEX IF NOT EXISTS idx_auditoria_modulo_fecha
    ON auditoria_eventos (modulo_auditoria, fecha_evento DESC);

-- Filtro por acción concreta (decenas de valores, buena selectividad).
CREATE INDEX IF NOT EXISTS idx_auditoria_accion_fecha
    ON auditoria_eventos (accion_auditoria, fecha_evento DESC);

-- Parcial a propósito: ~99% de las filas serán EXITO, así que un btree
-- completo sobre resultado_evento nunca lo elegiría el planner. El filtro que
-- de verdad se usa es "enséñame solo lo que falló o se denegó", y este índice
-- parcial diminuto lo resuelve sin tocar el resto de la tabla.
CREATE INDEX IF NOT EXISTS idx_auditoria_no_exitosos
    ON auditoria_eventos (fecha_evento DESC)
    WHERE resultado_evento <> 'EXITO';

-- Habilita "historial de auditoría de ESTA entidad" (p. ej. un pedido
-- concreto), que es lo que convierte la bitácora en útil y no solo en un log
-- plano. Parcial porque muchas acciones (login) no tienen entidad asociada.
CREATE INDEX IF NOT EXISTS idx_auditoria_entidad
    ON auditoria_eventos (entidad_afectada, id_entidad_afectada, fecha_evento DESC)
    WHERE entidad_afectada IS NOT NULL;

-- ------------------------------------------------------------------------------
-- 3. Trigger de inmutabilidad (RNF/REQ-NF-013)
-- ------------------------------------------------------------------------------
-- No va a db/procs/: ese directorio es el catálogo de rutinas de acceso a
-- datos invocadas desde Java (con entrada obligatoria en
-- docs/basedatos/CATALOGO-SP.md y verificadas por scripts/sync-procs.sh
-- --check en CI). Esta función no la llama nadie: es una restricción de
-- integridad de la propia tabla, igual que set_actualizado_en() de V2 vive en
-- su migración y no en db/procs/. Además R__procedimientos.sql se aplica
-- DESPUÉS de las migraciones versionadas en la misma corrida de Flyway, así
-- que un CREATE TRIGGER que dependiera de una función definida ahí fallaría
-- en una base virgen.
--
-- Se usa DROP TRIGGER IF EXISTS + CREATE TRIGGER (no CREATE OR REPLACE
-- TRIGGER) para seguir el idiom ya establecido por V2 y V3 en este proyecto.
CREATE OR REPLACE FUNCTION fn_bloquear_modificacion_auditoria()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'AUDITORIA_INMUTABLE: la bitacora de auditoria es de solo insercion (REQ-NF-013). Operacion % rechazada sobre %',
        TG_OP, TG_TABLE_NAME
        USING ERRCODE = '42501';
END;
$$;

DROP TRIGGER IF EXISTS trg_auditoria_eventos_inmutable ON auditoria_eventos;
CREATE TRIGGER trg_auditoria_eventos_inmutable
    BEFORE UPDATE OR DELETE ON auditoria_eventos
    FOR EACH ROW
    EXECUTE FUNCTION fn_bloquear_modificacion_auditoria();

-- PostgreSQL no admite BEFORE TRUNCATE ... FOR EACH ROW: tiene que ser
-- FOR EACH STATEMENT.
DROP TRIGGER IF EXISTS trg_auditoria_eventos_no_truncate ON auditoria_eventos;
CREATE TRIGGER trg_auditoria_eventos_no_truncate
    BEFORE TRUNCATE ON auditoria_eventos
    FOR EACH STATEMENT
    EXECUTE FUNCTION fn_bloquear_modificacion_auditoria();

-- ------------------------------------------------------------------------------
-- 4. GRANTs restringidos: la cuenta de aplicación solo puede leer e insertar
-- ------------------------------------------------------------------------------
-- El REVOKE es obligatorio, no decorativo, por dos motivos:
--
--   (a) seed_privilegios.sh ejecuta ALTER DEFAULT PRIVILEGES ... GRANT
--       SELECT, INSERT, UPDATE, DELETE ON TABLES, y ese default se aplica
--       tambien a las tablas que Flyway cree DESPUES (misma cuenta postgres).
--       Sin este REVOKE explicito, artisync_app nacería con permiso de
--       UPDATE y DELETE sobre la bitacora, y la unica defensa seria el
--       trigger.
--   (b) En el despliegue real (Azure PostgreSQL, ver artisync/.env.example),
--       artisync_app se crea A MANO y seed_privilegios.sh no corre como
--       init-script. Esta migracion es lo unico que se ejecuta en TODOS los
--       entornos, así que es la unica garantia universal.
--
-- El IF EXISTS replica el mismo patron condicional de V7 (líneas 147-154):
-- en una maquina de desarrollo donde seed_privilegios.sh aun no ha creado la
-- cuenta, un GRANT a un rol inexistente abortaria TODA la migracion V12.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_app') THEN
        REVOKE ALL ON auditoria_eventos FROM artisync_app;
        GRANT SELECT, INSERT ON auditoria_eventos TO artisync_app;
        GRANT USAGE, SELECT ON SEQUENCE auditoria_eventos_id_evento_auditoria_seq TO artisync_app;
    END IF;
END
$$;

-- ------------------------------------------------------------------------------
-- 5. Permisos nuevos y asignación a roles
-- ------------------------------------------------------------------------------
-- Módulo SEGURIDAD (agrupan con SESION_REVOCAR): la auditoría es una
-- capacidad de seguridad transversal, no un simple acceso a pantalla como los
-- de V10 (módulo SISTEMA).
INSERT INTO permisos (nombre_permiso, modulo_aplicacion)
VALUES
    ('AUDITORIA_VER',      'SEGURIDAD'),
    ('AUDITORIA_EXPORTAR', 'SEGURIDAD')
ON CONFLICT (nombre_permiso) DO UPDATE
SET modulo_aplicacion = EXCLUDED.modulo_aplicacion;

-- ADMIN: INSERT EXPLICITO. El cross-join de db/seed.sql que da todos los
-- permisos a ADMIN ya corrio como init-script ANTES de que Flyway aplicara
-- esta migracion (mismo motivo documentado en V10, lineas 19-22).
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'ADMIN'
  AND p.nombre_permiso IN ('AUDITORIA_VER', 'AUDITORIA_EXPORTAR')
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- AUDITOR_FINANCIERO: es el rol que por fin recibe una pantalla propia. Hasta
-- ahora tenía permisos financieros (PAGO_AUDITAR, TRANSACCION_VER, ...) sin
-- ninguna entrada en NAV_CATALOG que los usara.
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'AUDITOR_FINANCIERO'
  AND p.nombre_permiso IN ('AUDITORIA_VER', 'AUDITORIA_EXPORTAR')
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- SOPORTE ve pero no exporta: exportar es sacar datos personales del sistema
-- en un archivo, y eso es una capacidad más sensible que solo consultarlos.
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'SOPORTE'
  AND p.nombre_permiso = 'AUDITORIA_VER'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
