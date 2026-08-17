-- V6__personalizacion_colores_portafolio.sql

-- 1. Agregar nueva columna JSONB
ALTER TABLE portafolios ADD COLUMN IF NOT EXISTS opciones_personalizacion JSONB;

-- 2. Migrar datos existentes (opcional, mapeando color_plantilla actual al formato nuevo)
-- Asumimos que color_plantilla se convierte en el color 'primary'
UPDATE portafolios
SET opciones_personalizacion = jsonb_build_object(
    'primary', color_plantilla,
    'secondary', '#6c757d',
    'bg', '#f8f9fa',
    'text', '#212529',
    'surface', '#ffffff'
)
WHERE opciones_personalizacion IS NULL;

-- 3. Eliminar columna antigua
ALTER TABLE portafolios DROP COLUMN IF EXISTS color_plantilla;
