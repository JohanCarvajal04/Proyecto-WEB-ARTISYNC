-- ==============================================================================
-- MIGRACIÓN V10: PERMISOS DE NAVEGACIÓN PARA LAS PÁGINAS SIN CÓDIGO GRANULAR
-- ==============================================================================
--
-- El control de acceso del frontend pasa a decidirse por permisos y no por
-- nombre de rol, de modo que un rol creado desde "Roles y Permisos" reciba
-- exactamente las páginas que se le asignen. Cuatro pantallas del panel de
-- administración no tenían ningún permiso al que asociarse y quedaban como la
-- excepción atada al rol ADMIN:
--
--   * mod-overview   -> sin guard alguno: cualquier usuario del panel entraba.
--   * settings       -> sin guard alguno: idem.
--   * infracciones   -> canActivate por data.roles ['ADMIN','ADMINISTRADOR'].
--   * flujos         -> canActivate por data.roles ['ADMIN','ADMINISTRADOR'].
--
-- Con estos cuatro códigos, las 100% de las rutas del panel se gobiernan por
-- permiso y desaparecen los dos últimos guards por nombre de rol.
--
-- Idempotente a propósito: en el arranque de Docker `db/seed.sql` se ejecuta
-- como init-script ANTES de que Flyway aplique V6..V10, así que esta migración
-- convive con una base que ya tiene los 35 permisos originales. Los ON CONFLICT
-- permiten además re-ejecutarla sobre entornos ya migrados.

-- ------------------------------------------------------------------------------
-- 1. Nuevos permisos
-- ------------------------------------------------------------------------------
-- Todos en el módulo SISTEMA: no describen una entidad de negocio sino el
-- acceso a una pantalla de administración, igual que PAIS_VER.
INSERT INTO permisos (nombre_permiso, modulo_aplicacion)
VALUES
    ('PANEL_MODERACION_VER',    'SISTEMA'),
    ('CONFIGURACION_GESTIONAR', 'SISTEMA'),
    ('INFRACCION_GESTIONAR',    'SISTEMA'),
    ('FLUJO_GESTIONAR',         'SISTEMA')
ON CONFLICT (nombre_permiso) DO UPDATE
SET modulo_aplicacion = EXCLUDED.modulo_aplicacion;

-- ------------------------------------------------------------------------------
-- 2. Asignación al rol ADMIN
-- ------------------------------------------------------------------------------
-- El seed da a ADMIN todos los permisos con un cross join, pero ese INSERT ya
-- corrió antes que esta migración: sin este bloque, ADMIN perdería el acceso a
-- las cuatro pantallas en cuanto el frontend empiece a exigir el permiso.
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'ADMIN'
  AND p.nombre_permiso IN (
      'PANEL_MODERACION_VER', 'CONFIGURACION_GESTIONAR',
      'INFRACCION_GESTIONAR', 'FLUJO_GESTIONAR'
  )
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- ------------------------------------------------------------------------------
-- 3. Asignación al rol MODERADOR
-- ------------------------------------------------------------------------------
-- Solo el panel de moderación: hoy `mod-overview` es su pantalla de aterrizaje
-- y debe seguir siéndolo. Las otras tres son exclusivas de ADMIN, tal y como
-- estaban antes por rol.
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'MODERADOR'
  AND p.nombre_permiso = 'PANEL_MODERACION_VER'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
