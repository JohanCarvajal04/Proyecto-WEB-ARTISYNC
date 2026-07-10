-- ==============================================================================
-- MIGRACIÓN V3: AJUSTES EN CATÁLOGO DE SERVICIOS Y AUDITORÍA PFC
-- ==============================================================================

-- 1. Añadir columnas a la tabla servicios según Guía Módulo 3
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS tipo_item VARCHAR(20) NOT NULL DEFAULT 'SERVICIO';
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS estado_publicacion VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS cargo_revision_adicional DECIMAL(10,2) DEFAULT 0.00;
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS limite_revisiones_base INT DEFAULT 0;

-- 2. Añadir columna actualizado_en y trigger a la tabla servicios (Guía PFC)
ALTER TABLE servicios ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_servicios_actualizado_en ON servicios;
CREATE TRIGGER trg_servicios_actualizado_en
    BEFORE UPDATE ON servicios
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 3. Añadir columna actualizado_en y trigger a la tabla categorias
ALTER TABLE categorias ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_categorias_actualizado_en ON categorias;
CREATE TRIGGER trg_categorias_actualizado_en
    BEFORE UPDATE ON categorias
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 4. Añadir columna actualizado_en y trigger a la tabla subcategorias
ALTER TABLE subcategorias ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_subcategorias_actualizado_en ON subcategorias;
CREATE TRIGGER trg_subcategorias_actualizado_en
    BEFORE UPDATE ON subcategorias
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 5. Añadir columna actualizado_en y trigger a la tabla atributos_dinamicos
ALTER TABLE atributos_dinamicos ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_atributos_dinamicos_actualizado_en ON atributos_dinamicos;
CREATE TRIGGER trg_atributos_dinamicos_actualizado_en
    BEFORE UPDATE ON atributos_dinamicos
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 6. Añadir columna actualizado_en y trigger a la tabla servicio_atributos
ALTER TABLE servicio_atributos ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_servicio_atributos_actualizado_en ON servicio_atributos;
CREATE TRIGGER trg_servicio_atributos_actualizado_en
    BEFORE UPDATE ON servicio_atributos
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 7. Añadir columna actualizado_en y trigger a la tabla etiquetas
ALTER TABLE etiquetas ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_etiquetas_actualizado_en ON etiquetas;
CREATE TRIGGER trg_etiquetas_actualizado_en
    BEFORE UPDATE ON etiquetas
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 8. Añadir columna actualizado_en y trigger a la tabla servicio_etiquetas
ALTER TABLE servicio_etiquetas ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_servicio_etiquetas_actualizado_en ON servicio_etiquetas;
CREATE TRIGGER trg_servicio_etiquetas_actualizado_en
    BEFORE UPDATE ON servicio_etiquetas
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();
