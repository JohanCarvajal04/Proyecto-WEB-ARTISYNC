# Diccionario de datos

> Completar una tabla por cada tabla de la base de datos. Mantener sincronizado
> con los scripts Flyway en `backend/src/main/resources/db/migration/`.

## Tabla: usuarios

| Columna | Tipo PostgreSQL 16 | Nulo | Default | Descripción de negocio |
|---|---|---|---|---|
| id | BIGSERIAL | NO | autoincrement | Identificador único del usuario (PK) |
| nombre | VARCHAR(100) | NO | — | Nombre completo del usuario |
| email | VARCHAR(255) | NO | — | Correo, único; usado como `username` de login |
| password_hash | VARCHAR(255) | NO | — | Hash BCrypt (costo 12), nunca expuesto en respuestas JSON |
| rol | VARCHAR(20) | NO | 'ROLE_USER' | ROLE_USER o ROLE_ADMIN (CHECK constraint) |
| activo | BOOLEAN | NO | TRUE | Soft delete: FALSE = usuario inactivo/eliminado |
| creado_en | TIMESTAMPTZ | NO | NOW() | Fecha de creación del registro (UTC) |
| actualizado_en | TIMESTAMPTZ | NO | NOW() | Última modificación (actualizado por trigger) |

## Tabla: [entidad_principal]

| Columna | Tipo PostgreSQL 16 | Nulo | Default | Descripción de negocio |
|---|---|---|---|---|
| ... | ... | ... | ... | ... |
