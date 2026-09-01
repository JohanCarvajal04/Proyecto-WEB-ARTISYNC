-- Version manual de seed_privilegios.sh para ejecutar una sola vez contra la
-- base de datos administrada de Render (que no corre docker-entrypoint-initdb.d).
-- Reemplaza TU_DB_APP_PASSWORD por el mismo valor que pusiste en la variable
-- de entorno DB_APP_PASSWORD del servicio artisync-backend en Render.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'artisync_app') THEN
        CREATE ROLE artisync_app LOGIN PASSWORD 'TU_DB_APP_PASSWORD';
    END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO artisync_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO artisync_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO artisync_app;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO artisync_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO artisync_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO artisync_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT EXECUTE ON FUNCTIONS TO artisync_app;
