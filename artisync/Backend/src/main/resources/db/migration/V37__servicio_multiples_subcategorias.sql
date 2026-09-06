-- ==============================================================================
-- MIGRACIÓN V37: SERVICIO EN VARIAS SUBCATEGORÍAS
-- ==============================================================================
--
-- Hasta ahora `servicios.id_subcategoria` obligaba a un servicio a pertenecer
-- a exactamente una subcategoría. Se pide que un servicio pueda listarse bajo
-- varias. Se sigue el mismo patrón que ya usa este proyecto para relaciones
-- N:M de Servicio (ver `servicio_etiquetas`): una tabla puente explícita en
-- vez de un `@ManyToMany` directo.

CREATE TABLE IF NOT EXISTS servicio_subcategorias (
    id_servicio_subcategoria BIGSERIAL PRIMARY KEY,
    id_servicio BIGINT NOT NULL REFERENCES servicios(id_servicio) ON DELETE CASCADE,
    id_subcategoria BIGINT NOT NULL REFERENCES subcategorias(id_subcategoria),
    actualizado_en TIMESTAMP,
    UNIQUE (id_servicio, id_subcategoria)
);

CREATE INDEX IF NOT EXISTS idx_servicio_subcategorias_servicio ON servicio_subcategorias (id_servicio);
CREATE INDEX IF NOT EXISTS idx_servicio_subcategorias_subcategoria ON servicio_subcategorias (id_subcategoria);

-- Backfill: cada servicio existente conserva su única subcategoría de hoy.
INSERT INTO servicio_subcategorias (id_servicio, id_subcategoria)
SELECT id_servicio, id_subcategoria FROM servicios WHERE id_subcategoria IS NOT NULL;

-- Postgres retira solas la FK y el índice viejos de servicios.id_subcategoria
-- al eliminar la columna.
ALTER TABLE servicios DROP COLUMN IF EXISTS id_subcategoria;
