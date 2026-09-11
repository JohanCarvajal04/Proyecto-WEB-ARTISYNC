-- Version manual de la seccion "artisync_backup" de seed_privilegios.sh para
-- ejecutar una sola vez contra la base de datos administrada de Render.
-- Reemplaza TU_DB_BACKUP_PASSWORD por el mismo valor que pondras en la
-- variable de entorno DB_BACKUP_PASSWORD del servicio artisync-backend.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'artisync_backup') THEN
        CREATE ROLE artisync_backup LOGIN PASSWORD 'TU_DB_BACKUP_PASSWORD';
    END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO artisync_backup;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO artisync_backup;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO artisync_backup;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO artisync_backup;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO artisync_backup;
