#!/bin/bash
# ==============================================================================
# ARTISYNC — Cuenta de aplicacion con privilegios minimos (A.2.3 de la guia)
# ==============================================================================
#
# La cuenta con la que el backend se conecta a Postgres NO debe ser el
# superusuario (POSTGRES_USER, usado solo para inicializar el contenedor).
# Se le otorgan unicamente los privilegios que la aplicacion necesita: CRUD
# sobre las tablas del dominio y EXECUTE sobre procedimientos/funciones
# (db/procs/, cuando existan). Sin DBA, sin OWNER, sin superuser.
#
# Se ejecuta como .sh (no .sql) porque necesita leer DB_APP_PASSWORD del
# entorno del contenedor; los scripts .sql puros montados en
# /docker-entrypoint-initdb.d/ no tienen acceso a variables de shell.
# Postgres ejecuta los archivos de esa carpeta en orden alfabetico: este
# nombre (seed_privilegios.sh) ordena despues de schema.sql y seed.sql.
set -euo pipefail

: "${DB_APP_PASSWORD:?Falta la variable de entorno DB_APP_PASSWORD}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'artisync_app') THEN
            CREATE ROLE artisync_app LOGIN PASSWORD '$DB_APP_PASSWORD';
        END IF;
    END
    \$\$;

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
EOSQL
