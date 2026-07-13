-- ==============================================================================
-- SCRIPT DE SEGURIDAD COMPLETO - ArtiSync
-- Base de datos: artisyncbd | PostgreSQL 16
-- Ejecutar como superusuario (postgres):
--   psql -U postgres -f seguridad_artisync.sql
-- ==============================================================================

-- ==============================================================================
-- FASE 0: CREACIÓN DE LA BASE DE DATOS
-- ==============================================================================

-- Eliminar la base de datos si ya existe (CUIDADO en producción)
-- DROP DATABASE IF EXISTS artisyncbd;

-- Crear la base de datos
CREATE DATABASE artisyncbd
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'es_EC.UTF-8'
    LC_CTYPE = 'es_EC.UTF-8'
    LOCALE_PROVIDER = 'libc'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1
    IS_TEMPLATE = False;

COMMENT ON DATABASE artisyncbd IS
    'Base de datos principal de la plataforma ArtiSync - '
    'Marketplace creativo para artistas digitales.';

-- Conectar a la base de datos recién creada
\connect artisyncbd;

-- ==============================================================================
-- FASE 1: LIMPIEZA PREVIA (IDEMPOTENTE)
-- ==============================================================================
DO $$
BEGIN
    -- Eliminar usuarios de login si existen
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_readonly') THEN
        REASSIGN OWNED BY artisync_readonly TO postgres;
        DROP OWNED BY artisync_readonly;
        DROP ROLE artisync_readonly;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_backup') THEN
        REASSIGN OWNED BY artisync_backup TO postgres;
        DROP OWNED BY artisync_backup;
        DROP ROLE artisync_backup;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_auditor') THEN
        REASSIGN OWNED BY artisync_auditor TO postgres;
        DROP OWNED BY artisync_auditor;
        DROP ROLE artisync_auditor;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_soporte') THEN
        REASSIGN OWNED BY artisync_soporte TO postgres;
        DROP OWNED BY artisync_soporte;
        DROP ROLE artisync_soporte;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_moderador') THEN
        REASSIGN OWNED BY artisync_moderador TO postgres;
        DROP OWNED BY artisync_moderador;
        DROP ROLE artisync_moderador;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_admin') THEN
        REASSIGN OWNED BY artisync_admin TO postgres;
        DROP OWNED BY artisync_admin;
        DROP ROLE artisync_admin;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_app') THEN
        REASSIGN OWNED BY artisync_app TO postgres;
        DROP OWNED BY artisync_app;
        DROP ROLE artisync_app;
    END IF;

    -- Eliminar roles de grupo si existen
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rol_solo_lectura') THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM rol_solo_lectura;
        REVOKE ALL PRIVILEGES ON SCHEMA public FROM rol_solo_lectura;
        DROP ROLE rol_solo_lectura;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rol_backup') THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM rol_backup;
        DROP ROLE rol_backup;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rol_auditor_fin') THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM rol_auditor_fin;
        REVOKE ALL PRIVILEGES ON SCHEMA public FROM rol_auditor_fin;
        DROP ROLE rol_auditor_fin;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rol_soporte') THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM rol_soporte;
        REVOKE ALL PRIVILEGES ON SCHEMA public FROM rol_soporte;
        DROP ROLE rol_soporte;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rol_moderador') THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM rol_moderador;
        REVOKE ALL PRIVILEGES ON SCHEMA public FROM rol_moderador;
        DROP ROLE rol_moderador;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rol_administrador') THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM rol_administrador;
        REVOKE ALL PRIVILEGES ON SCHEMA public FROM rol_administrador;
        DROP ROLE rol_administrador;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rol_app_backend') THEN
        REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM rol_app_backend;
        REVOKE ALL PRIVILEGES ON SCHEMA public FROM rol_app_backend;
        DROP ROLE rol_app_backend;
    END IF;

    RAISE NOTICE 'Limpieza previa completada.';
END $$;


-- ==============================================================================
-- FASE 2: CREACIÓN DE ROLES DE GRUPO (NOLOGIN)
-- ==============================================================================

-- 2.1 Rol para la aplicación backend (Spring Boot / JPA)
CREATE ROLE rol_app_backend
    NOLOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT;

COMMENT ON ROLE rol_app_backend IS
    'Rol de grupo para el servicio backend ArtiSync. '
    'Acceso DML completo (SELECT, INSERT, UPDATE, DELETE) '
    'sobre todas las tablas del esquema public.';

-- 2.2 Rol de administrador del sistema
CREATE ROLE rol_administrador
    NOLOGIN
    NOSUPERUSER
    CREATEDB
    CREATEROLE
    INHERIT;

COMMENT ON ROLE rol_administrador IS
    'Rol de administrador con acceso total al esquema '
    'y capacidad de crear roles y bases de datos.';

-- Herencia: el administrador hereda del rol backend
GRANT rol_app_backend TO rol_administrador;

-- 2.3 Rol de moderador
CREATE ROLE rol_moderador
    NOLOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT;

COMMENT ON ROLE rol_moderador IS
    'Rol de moderador. Lectura general y escritura '
    'limitada a tablas de moderacion y verificacion.';

-- 2.4 Rol de soporte técnico
CREATE ROLE rol_soporte
    NOLOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT;

COMMENT ON ROLE rol_soporte IS
    'Rol de soporte tecnico. Solo lectura sobre '
    'usuarios, pedidos, tickets y comunicaciones.';

-- 2.5 Rol de auditor financiero
CREATE ROLE rol_auditor_fin
    NOLOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT;

COMMENT ON ROLE rol_auditor_fin IS
    'Rol de auditoria financiera. Solo lectura '
    'sobre tablas de pagos, contratos y transacciones.';

-- 2.6 Rol de backup
CREATE ROLE rol_backup
    NOLOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT;

COMMENT ON ROLE rol_backup IS
    'Rol para respaldos automatizados. Lectura '
    'completa via pg_read_all_data para pg_dump.';

-- Otorgar pg_read_all_data al rol de backup
GRANT pg_read_all_data TO rol_backup;

-- 2.7 Rol de solo lectura (monitoreo/BI)
CREATE ROLE rol_solo_lectura
    NOLOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT;

COMMENT ON ROLE rol_solo_lectura IS
    'Rol de solo lectura para dashboards, '
    'Grafana, Metabase y herramientas de BI.';


-- ==============================================================================
-- FASE 3: CREACIÓN DE USUARIOS DE LOGIN
-- IMPORTANTE: Cambiar las contraseñas antes de usar en producción
-- ==============================================================================

-- 3.1 Usuario de la aplicación backend
CREATE ROLE artisync_app
    LOGIN
    PASSWORD 'App$ecure2026!'
    NOSUPERUSER
    INHERIT
    CONNECTION LIMIT 20
    VALID UNTIL '2027-12-31';

GRANT rol_app_backend TO artisync_app;

COMMENT ON ROLE artisync_app IS
    'Usuario de conexion JDBC para el servicio '
    'Spring Boot de ArtiSync.';

-- 3.2 Usuario administrador
CREATE ROLE artisync_admin
    LOGIN
    PASSWORD 'Adm!nStr0ng2026'
    NOSUPERUSER
    INHERIT
    CONNECTION LIMIT 3;

GRANT rol_administrador TO artisync_admin;

COMMENT ON ROLE artisync_admin IS
    'Usuario administrador para gestion directa '
    'de la base de datos ArtiSync.';

-- 3.3 Usuario moderador
CREATE ROLE artisync_moderador
    LOGIN
    PASSWORD 'M0d3r@t0r2026'
    NOSUPERUSER
    INHERIT
    CONNECTION LIMIT 5;

GRANT rol_moderador TO artisync_moderador;

COMMENT ON ROLE artisync_moderador IS
    'Usuario del panel de moderacion de contenido.';

-- 3.4 Usuario soporte
CREATE ROLE artisync_soporte
    LOGIN
    PASSWORD 'S0p0rt3$2026'
    NOSUPERUSER
    INHERIT
    CONNECTION LIMIT 5;

GRANT rol_soporte TO artisync_soporte;

COMMENT ON ROLE artisync_soporte IS
    'Usuario del equipo de soporte tecnico.';

-- 3.5 Usuario auditor financiero
CREATE ROLE artisync_auditor
    LOGIN
    PASSWORD 'Aud!t0r2026$'
    NOSUPERUSER
    INHERIT
    CONNECTION LIMIT 2;

GRANT rol_auditor_fin TO artisync_auditor;

COMMENT ON ROLE artisync_auditor IS
    'Usuario para auditoria y reportes financieros.';

-- 3.6 Usuario de backup
CREATE ROLE artisync_backup
    LOGIN
    PASSWORD 'B@ckup$2026Secure'
    NOSUPERUSER
    INHERIT
    CONNECTION LIMIT 2;

GRANT rol_backup TO artisync_backup;

COMMENT ON ROLE artisync_backup IS
    'Usuario para tareas automatizadas de pg_dump.';

-- 3.7 Usuario de solo lectura
CREATE ROLE artisync_readonly
    LOGIN
    PASSWORD 'R3@d0nly2026!'
    NOSUPERUSER
    INHERIT
    CONNECTION LIMIT 10;

GRANT rol_solo_lectura TO artisync_readonly;

COMMENT ON ROLE artisync_readonly IS
    'Usuario para herramientas de BI y monitoreo.';


-- ==============================================================================
-- FASE 4: PRIVILEGIOS SOBRE EL ESQUEMA
-- ==============================================================================

-- 4.1 Revocar privilegios por defecto del esquema public
REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM PUBLIC;

-- 4.2 Otorgar USAGE del esquema a todos los roles
GRANT USAGE ON SCHEMA public TO rol_app_backend;
GRANT USAGE ON SCHEMA public TO rol_moderador;
GRANT USAGE ON SCHEMA public TO rol_soporte;
GRANT USAGE ON SCHEMA public TO rol_auditor_fin;
GRANT USAGE ON SCHEMA public TO rol_solo_lectura;

-- 4.3 Privilegio CREATE en esquema solo para admin
GRANT CREATE ON SCHEMA public TO rol_administrador;


-- ==============================================================================
-- FASE 5: PRIVILEGIOS DE rol_app_backend (DML COMPLETO)
-- ==============================================================================

-- 5.1 SELECT, INSERT, UPDATE, DELETE sobre TODAS las tablas
GRANT SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public
    TO rol_app_backend;

-- 5.2 USAGE sobre todas las secuencias (SERIAL/BIGSERIAL)
GRANT USAGE, SELECT
    ON ALL SEQUENCES IN SCHEMA public
    TO rol_app_backend;

-- 5.3 EXECUTE sobre funciones PL/pgSQL
GRANT EXECUTE
    ON ALL FUNCTIONS IN SCHEMA public
    TO rol_app_backend;

-- 5.4 Privilegios por defecto para tablas futuras
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE
    ON TABLES TO rol_app_backend;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT
    ON SEQUENCES TO rol_app_backend;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT EXECUTE
    ON FUNCTIONS TO rol_app_backend;


-- ==============================================================================
-- FASE 6: PRIVILEGIOS DE rol_administrador (ALL + DDL)
-- Ya hereda DML de rol_app_backend
-- ==============================================================================

-- 6.1 ALL PRIVILEGES sobre todas las tablas
GRANT ALL PRIVILEGES
    ON ALL TABLES IN SCHEMA public
    TO rol_administrador;

-- 6.2 ALL sobre secuencias
GRANT ALL PRIVILEGES
    ON ALL SEQUENCES IN SCHEMA public
    TO rol_administrador;

-- 6.3 ALL sobre funciones
GRANT ALL PRIVILEGES
    ON ALL FUNCTIONS IN SCHEMA public
    TO rol_administrador;

-- 6.4 Privilegios por defecto para objetos futuros
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL PRIVILEGES
    ON TABLES TO rol_administrador;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL PRIVILEGES
    ON SEQUENCES TO rol_administrador;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL PRIVILEGES
    ON FUNCTIONS TO rol_administrador;


-- ==============================================================================
-- FASE 7: PRIVILEGIOS DE rol_moderador (LECTURA + ESCRITURA LIMITADA)
-- ==============================================================================

-- 7.1 Tablas de solo lectura
GRANT SELECT ON
    usuarios,
    perfiles_creadores,
    portafolio_items,
    portafolios,
    servicios,
    likes_portafolio,
    estados_verificacion,
    habilidades,
    creador_habilidades
TO rol_moderador;

-- 7.2 Certificados IA: revisar y actualizar estado
GRANT SELECT, UPDATE ON certificados_ia
    TO rol_moderador;

-- 7.3 Comentarios: moderar (actualizar estado, eliminar)
GRANT SELECT, UPDATE, DELETE ON comentarios_portafolio
    TO rol_moderador;

-- 7.4 Sorteos: supervisar y suspender
GRANT SELECT, UPDATE, DELETE ON sorteos
    TO rol_moderador;

GRANT SELECT ON participantes_sorteo
    TO rol_moderador;

-- 7.5 Secuencias necesarias
GRANT USAGE ON SEQUENCE certificados_ia_id_certificado_seq
    TO rol_moderador;


-- ==============================================================================
-- FASE 8: PRIVILEGIOS DE rol_soporte (SOLO LECTURA OPERATIVA)
-- ==============================================================================

GRANT SELECT ON
    usuarios,
    usuario_roles,
    roles,
    perfiles_creadores,
    sesiones_usuario,
    tokens_recuperacion,
    pedidos,
    historial_estados_pedido,
    tickets_revision,
    motivos_rechazo,
    salas_chat,
    mensajes,
    documentos_adjuntos,
    notificaciones_sistema,
    tipos_notificacion,
    servicios,
    categorias,
    subcategorias,
    contratos
TO rol_soporte;


-- ==============================================================================
-- FASE 9: PRIVILEGIOS DE rol_auditor_fin (SOLO LECTURA FINANCIERA)
-- ==============================================================================

GRANT SELECT ON
    contratos,
    plantillas_contrato,
    pagos_garantia,
    transacciones_pago,
    pedidos,
    entregables_finales,
    servicios,
    usuarios
TO rol_auditor_fin;


-- ==============================================================================
-- FASE 10: PRIVILEGIOS DE rol_solo_lectura (SELECT GLOBAL)
-- ==============================================================================

GRANT SELECT
    ON ALL TABLES IN SCHEMA public
    TO rol_solo_lectura;

-- Privilegios por defecto para tablas futuras
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT
    ON TABLES TO rol_solo_lectura;


-- ==============================================================================
-- FASE 11: REVOCACIÓN EXPLÍCITA Y HARDENING
-- ==============================================================================

-- 11.1 Revocar CONNECT a la base de datos para PUBLIC
REVOKE CONNECT ON DATABASE artisyncbd FROM PUBLIC;

-- 11.2 Otorgar CONNECT solo a roles autorizados
GRANT CONNECT ON DATABASE artisyncbd TO rol_app_backend;
GRANT CONNECT ON DATABASE artisyncbd TO rol_administrador;
GRANT CONNECT ON DATABASE artisyncbd TO rol_moderador;
GRANT CONNECT ON DATABASE artisyncbd TO rol_soporte;
GRANT CONNECT ON DATABASE artisyncbd TO rol_auditor_fin;
GRANT CONNECT ON DATABASE artisyncbd TO rol_backup;
GRANT CONNECT ON DATABASE artisyncbd TO rol_solo_lectura;

-- 11.3 Revocar CREATE sobre el esquema para roles no admin
REVOKE CREATE ON SCHEMA public FROM rol_app_backend;
REVOKE CREATE ON SCHEMA public FROM rol_moderador;
REVOKE CREATE ON SCHEMA public FROM rol_soporte;
REVOKE CREATE ON SCHEMA public FROM rol_auditor_fin;
REVOKE CREATE ON SCHEMA public FROM rol_solo_lectura;

-- 11.4 Revocar TRUNCATE para evitar eliminaciones masivas
REVOKE TRUNCATE ON ALL TABLES IN SCHEMA public
    FROM rol_app_backend;
REVOKE TRUNCATE ON ALL TABLES IN SCHEMA public
    FROM rol_moderador;


-- ==============================================================================
-- FASE 12: CONSULTAS DE VERIFICACIÓN
-- ==============================================================================

-- 12.1 Listar todos los roles creados
SELECT rolname, rolsuper, rolcreaterole, rolcreatedb,
       rolcanlogin, rolconnlimit
FROM pg_roles
WHERE rolname LIKE 'artisync_%'
   OR rolname LIKE 'rol_%';

-- 12.2 Verificar membresías de roles
SELECT r.rolname AS usuario,
       m.rolname AS miembro_de
FROM pg_auth_members am
JOIN pg_roles r ON am.member = r.oid
JOIN pg_roles m ON am.roleid = m.oid
WHERE r.rolname LIKE 'artisync_%';

-- 12.3 Verificar privilegios sobre tablas
SELECT grantee, table_name, privilege_type
FROM information_schema.table_privileges
WHERE table_schema = 'public'
  AND grantee LIKE 'rol_%'
ORDER BY grantee, table_name, privilege_type;

-- 12.4 Verificar privilegios sobre secuencias
SELECT grantee, object_name, privilege_type
FROM information_schema.usage_privileges
WHERE object_schema = 'public'
  AND grantee LIKE 'rol_%'
ORDER BY grantee, object_name;

-- 12.5 Verificar que PUBLIC no tiene acceso
SELECT grantee, table_name, privilege_type
FROM information_schema.table_privileges
WHERE table_schema = 'public'
  AND grantee = 'PUBLIC';

-- ==============================================================================
-- FIN DEL SCRIPT DE SEGURIDAD
-- ==============================================================================
