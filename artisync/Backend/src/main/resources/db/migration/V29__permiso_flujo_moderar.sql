-- ==============================================================================
-- MIGRACIÓN V26: PERMISO FLUJO_MODERAR (visibilidad de flujos de todos los creadores)
-- ==============================================================================
--
-- FLUJO_GESTIONAR (V10) quedó definido como "gestiona TUS flujos" (V25 hizo a
-- cada FlujoTrabajo propiedad de un creador, filtrado por id_usuario_creador).
-- Pero el selector de flujo dentro de Categorías necesita listar los flujos de
-- TODOS los creadores para poder asignar uno a una categoría, y un ADMIN sin
-- flujos propios no tiene nada que gestionar bajo FLUJO_GESTIONAR.
--
-- En vez de sobrecargar un solo permiso con dos alcances distintos (propio vs
-- todos), se sigue el mismo patrón que ya usa el proyecto para separar
-- autoservicio de supervisión (SERVICIO_CREAR/SERVICIO_MODERAR,
-- PORTAFOLIO_CREAR/PORTAFOLIO_MODERAR, MENSAJE_ENVIAR/MENSAJE_MODERAR):
-- FLUJO_GESTIONAR sigue siendo "los míos", FLUJO_MODERAR es "todos".
--
-- Idempotente por el mismo motivo que V10 (convive con db/seed.sql aplicado en
-- el arranque de Docker).

INSERT INTO permisos (nombre_permiso, modulo_aplicacion)
VALUES
    ('FLUJO_MODERAR', 'SISTEMA')
ON CONFLICT (nombre_permiso) DO UPDATE
SET modulo_aplicacion = EXCLUDED.modulo_aplicacion;

INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'ADMIN'
  AND p.nombre_permiso = 'FLUJO_MODERAR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
