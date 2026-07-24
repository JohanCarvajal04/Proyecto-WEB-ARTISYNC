-- =============================================================================
-- V5: Módulo 7 — Social, Comunidad y Sorteos
-- RF-23: Sorteos configurables con selección automática de ganadores
-- RF-09: Reseñas y calificaciones de servicios (complemento)
-- RNF-13: Exportación de historial de transacciones en CSV
-- =============================================================================

-- -----------------------------------------------------------------------------
-- RF-23: Columna adicional en sorteos (requiere_seguidor)
-- La tabla 'sorteos' y 'participantes_sorteo' ya existen desde V1.
-- Solo se agrega el campo que faltaba para el requisito de seguidor.
-- -----------------------------------------------------------------------------
ALTER TABLE sorteos
    ADD COLUMN IF NOT EXISTS requiere_seguidor BOOLEAN NOT NULL DEFAULT FALSE;
