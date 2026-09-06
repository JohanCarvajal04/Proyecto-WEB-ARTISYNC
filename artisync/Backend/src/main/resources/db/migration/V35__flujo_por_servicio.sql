-- ==============================================================================
-- MIGRACIÓN V35: FLUJO DE TRABAJO POR SERVICIO (reemplaza flujo por categoría)
-- ==============================================================================
--
-- V9 (RF-19) le dio flujo a la Categoría, pero una categoría solo existe para
-- que el creador se identifique con un rubro y para clasificar el catálogo:
-- no tiene sentido de negocio que "cargue" un flujo de trabajo. El flujo lo
-- crea y asigna el propio creador, y pertenece al servicio concreto que
-- ofrece -- no a la categoría bajo la que lo publicó.
--
-- Esta migración mueve la relación de `categorias` a `servicios` y borra la
-- columna vieja. `V28__flujos_por_creador.sql` ya había hecho que cada
-- FlujoTrabajo perteneciera a un creador; esto completa esa pieza: el
-- creador elige, entre sus propios flujos, cuál usa cada servicio.

-- ------------------------------------------------------------------------------
-- 1. Relación servicio → flujo
-- ------------------------------------------------------------------------------
-- Nullable a propósito, igual que lo era en categorias: un servicio sin flujo
-- asignado no debe bloquear la creación de un pedido (cae a un flujo de
-- respaldo en PedidoServicioImpl).
ALTER TABLE servicios
    ADD COLUMN IF NOT EXISTS id_flujo BIGINT REFERENCES flujos_trabajo(id_flujo);

CREATE INDEX IF NOT EXISTS idx_servicios_id_flujo ON servicios (id_flujo);

-- ------------------------------------------------------------------------------
-- 2. Backfill: conservar el flujo que ya tenía la categoría del servicio
-- ------------------------------------------------------------------------------
UPDATE servicios s
SET id_flujo = c.id_flujo
FROM subcategorias sub
JOIN categorias c ON c.id_categoria = sub.id_categoria
WHERE sub.id_subcategoria = s.id_subcategoria
  AND c.id_flujo IS NOT NULL
  AND s.id_flujo IS NULL;

-- ------------------------------------------------------------------------------
-- 3. Retirar la relación vieja
-- ------------------------------------------------------------------------------
DROP INDEX IF EXISTS idx_categorias_id_flujo;
ALTER TABLE categorias DROP COLUMN IF EXISTS id_flujo;

-- ------------------------------------------------------------------------------
-- 4. FLUJO_GESTIONAR para el rol CREADOR
-- ------------------------------------------------------------------------------
-- FLUJO_GESTIONAR (V10) solo se le había dado a ADMIN (y a MODERADOR en V32):
-- ningún CREADOR real lo tenía, así que "Mis Flujos de Trabajo" (panel creador)
-- devolvía 403 pese a estar montado en el menú. Ahora que el creador elige el
-- flujo directamente al crear/editar su servicio, necesita poder gestionar sus
-- propios flujos de verdad.
INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'CREADOR'
  AND p.nombre_permiso = 'FLUJO_GESTIONAR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
