-- ==============================================================================
-- MIGRACIÓN V2: AJUSTES PARA CUMPLIR REQUISITOS OFICIALES DE LA GUÍA PFC
-- ==============================================================================

-- 1. Función PL/pgSQL obligatoria para actualizar actualizado_en en cada UPDATE
CREATE OR REPLACE FUNCTION set_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Añadir columna actualizado_en a la tabla usuarios
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- 3. Trigger obligatorio para la tabla usuarios
DROP TRIGGER IF EXISTS trg_usuarios_actualizado_en ON usuarios;
CREATE TRIGGER trg_usuarios_actualizado_en
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();

-- 4. Añadir columna actualizado_en y trigger a la entidad portafolios
ALTER TABLE portafolios ADD COLUMN IF NOT EXISTS actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_portafolios_actualizado_en ON portafolios;
CREATE TRIGGER trg_portafolios_actualizado_en
    BEFORE UPDATE ON portafolios
    FOR EACH ROW
    EXECUTE FUNCTION set_actualizado_en();
