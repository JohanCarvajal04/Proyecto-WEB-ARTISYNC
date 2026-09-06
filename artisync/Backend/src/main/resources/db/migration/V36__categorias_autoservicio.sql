-- ==============================================================================
-- MIGRACIÓN V36: CATEGORÍAS/SUBCATEGORÍAS DE AUTOSERVICIO
-- ==============================================================================
--
-- Hasta ahora solo un ADMIN/MODERADOR (CATEGORIA_GESTIONAR) podía crear una
-- categoría o subcategoría. Se pide que el creador pueda crearlas él mismo
-- cuando su rubro no está en el catálogo, y que el moderador las revise
-- después -- mismo patrón que ya usa el proyecto para separar autoservicio de
-- supervisión (SERVICIO_CREAR/SERVICIO_MODERAR, PORTAFOLIO_CREAR/
-- PORTAFOLIO_MODERAR, MENSAJE_ENVIAR/MENSAJE_MODERAR).
--
-- Sin estado "pendiente" que bloquee el uso: la categoría/subcategoría queda
-- disponible de inmediato. Para que el moderador no tenga que revisar todo el
-- catálogo a ojo, cada una creada por un creador nace con revisado=false y
-- aparece en una cola de pendientes; el moderador la marca revisada o la
-- elimina con motivo (notificando a quien la creó).

-- ------------------------------------------------------------------------------
-- 1. Dueño opcional + bandera de revisión
-- ------------------------------------------------------------------------------
-- id_usuario_creador NULL = la creó un admin/moderador (como siempre; ya
-- confiable). revisado=true por defecto: las filas existentes no necesitan
-- pasar por la cola nueva.
ALTER TABLE categorias
    ADD COLUMN IF NOT EXISTS id_usuario_creador BIGINT REFERENCES usuarios(id_usuario),
    ADD COLUMN IF NOT EXISTS revisado BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE subcategorias
    ADD COLUMN IF NOT EXISTS id_usuario_creador BIGINT REFERENCES usuarios(id_usuario),
    ADD COLUMN IF NOT EXISTS revisado BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_categorias_revisado ON categorias (revisado) WHERE revisado = FALSE;
CREATE INDEX IF NOT EXISTS idx_subcategorias_revisado ON subcategorias (revisado) WHERE revisado = FALSE;

-- ------------------------------------------------------------------------------
-- 2. Permiso CATEGORIA_CREAR para el rol CREADOR
-- ------------------------------------------------------------------------------
-- CATEGORIA_GESTIONAR sigue siendo "control total" (crear/editar/eliminar
-- cualquiera); CATEGORIA_CREAR es autoservicio: cada creador solo puede crear
-- las suyas (eliminar y editar siguen exigiendo CATEGORIA_GESTIONAR).
INSERT INTO permisos (nombre_permiso, modulo_aplicacion)
VALUES
    ('CATEGORIA_CREAR', 'CATALOGO')
ON CONFLICT (nombre_permiso) DO UPDATE
SET modulo_aplicacion = EXCLUDED.modulo_aplicacion;

INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'CREADOR'
  AND p.nombre_permiso = 'CATEGORIA_CREAR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
