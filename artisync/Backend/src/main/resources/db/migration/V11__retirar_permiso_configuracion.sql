-- ==============================================================================
-- MIGRACIÓN V11: RETIRA EL PERMISO CONFIGURACION_GESTIONAR
-- ==============================================================================
--
-- V10 creó este permiso para gobernar la pantalla `admin/settings`. Esa pantalla
-- se eliminó al unificar la configuración personal en `/cuenta/configuracion`,
-- que es accesible sin ningún permiso, así que el código quedó sin ninguna ruta
-- ni componente que lo consuma.
--
-- Un permiso huérfano no es inocuo en esta aplicación: "Roles y Permisos" lista
-- el catálogo entero, de modo que un administrador seguiría viendo una casilla
-- asignable que no abre absolutamente nada.
--
-- No se toca V10: Flyway ya la aplicó y validaría el checksum. La retirada se
-- hace aquí, después.
--
-- Idempotente: ambos DELETE son no-ops si el permiso ya no existe.

-- 1. Primero las asignaciones a roles (FK hacia permisos).
DELETE FROM rol_permisos
WHERE id_permiso IN (
    SELECT id_permiso FROM permisos WHERE nombre_permiso = 'CONFIGURACION_GESTIONAR'
);

-- 2. Después el permiso.
DELETE FROM permisos
WHERE nombre_permiso = 'CONFIGURACION_GESTIONAR';
