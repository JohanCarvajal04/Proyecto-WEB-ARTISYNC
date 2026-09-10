-- ==============================================================================
-- ARTISYNC — Permiso dedicado para descarga de respaldos (REQ-NF-024)
-- ==============================================================================

-- El endpoint GET /api/v1/admin/respaldos/{id}/descargar reutilizaba RESPALDO_CREAR.
-- Se agrega un permiso propio para separar la autorización de "crear" de "descargar".
INSERT INTO permisos (nombre_permiso, modulo_aplicacion) VALUES
('RESPALDO_DESCARGAR', 'SISTEMA');

-- Asignar a ADMIN
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'ADMIN' AND p.nombre_permiso = 'RESPALDO_DESCARGAR';
