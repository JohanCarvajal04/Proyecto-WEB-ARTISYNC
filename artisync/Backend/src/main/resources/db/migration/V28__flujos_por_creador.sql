-- ==========================================
-- V25: Flujos de trabajo gestionados por Creador
-- ==========================================

-- 1. Añadir el dueño al flujo de trabajo
ALTER TABLE flujos_trabajo 
    ADD COLUMN id_usuario_creador BIGINT REFERENCES usuarios(id_usuario) ON DELETE CASCADE;

-- Como hay datos existentes (probablemente el flujo base), necesitamos asignar 
-- un dueño por defecto para poder hacer la columna NOT NULL. 
DO $$
DECLARE
    v_admin_id BIGINT;
BEGIN
    SELECT id_usuario INTO v_admin_id FROM usuarios LIMIT 1;
    
    IF v_admin_id IS NOT NULL THEN
        UPDATE flujos_trabajo SET id_usuario_creador = v_admin_id WHERE id_usuario_creador IS NULL;
    END IF;
END $$;

ALTER TABLE flujos_trabajo ALTER COLUMN id_usuario_creador SET NOT NULL;

-- 2. Restricción de Flujo Único por Creador
ALTER TABLE flujos_trabajo 
    ADD CONSTRAINT uk_flujos_trabajo_creador_nombre UNIQUE (id_usuario_creador, nombre_flujo);

-- 3. Restricción de Etapa Única por Flujo
-- No permitir que el mismo flujo tenga la misma etapa dos veces
ALTER TABLE flujo_etapas_config 
    ADD CONSTRAINT uk_flujo_etapas_config_unica UNIQUE (id_flujo, id_etapa);
