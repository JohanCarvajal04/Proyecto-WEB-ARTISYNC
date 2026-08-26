-- ------------------------------------------------------------------------------
-- Motor comun de exportacion de reportes (PDF/CSV/XLSX): permiso de exportar
-- para el listado administrativo de usuarios. Mismo criterio que
-- V19__permisos_reportes.sql: exportar es sacar datos del sistema en un
-- archivo, es una capacidad mas sensible que solo consultarlos (USUARIO_VER),
-- asi que es un permiso aparte.
-- ------------------------------------------------------------------------------

-- ADMIN: INSERT EXPLICITO. El cross-join de db/seed.sql que da todos los
-- permisos a ADMIN ya corrio como init-script ANTES de que Flyway aplicara
-- esta migracion (mismo motivo documentado en V10, V15 y V19).
INSERT INTO permisos (nombre_permiso, modulo_aplicacion)
VALUES
    ('USUARIO_EXPORTAR', 'SEGURIDAD')
ON CONFLICT (nombre_permiso) DO UPDATE
SET modulo_aplicacion = EXCLUDED.modulo_aplicacion;

INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'ADMIN'
  AND p.nombre_permiso = 'USUARIO_EXPORTAR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- SOPORTE ya tiene USUARIO_VER (V1__schema_inicial.sql) para consultar
-- usuarios en su labor de asistencia; se le da tambien la exportacion.
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'SOPORTE'
  AND p.nombre_permiso = 'USUARIO_EXPORTAR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
