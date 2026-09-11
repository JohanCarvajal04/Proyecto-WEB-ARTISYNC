-- =============================================================================
-- V46: reconciliacion activa contra PayPal y reembolso/liberacion de escrow
-- ante cancelacion con fondos retenidos (REQ-NF-019).
-- =============================================================================
-- pagos_garantia no tenia ninguna columna de fecha: no habia forma de saber
-- cuanto tiempo llevaba una fila en 'Pendiente' para poder reconciliarla
-- contra la API de PayPal cuando el webhook no llega. Tampoco existia donde
-- registrar el motivo de un reembolso fallido (mismo idioma que
-- solicitudes_retiro.mensaje_error, ver V del modulo de retiros).
-- =============================================================================

ALTER TABLE pagos_garantia
    ADD COLUMN IF NOT EXISTS fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS mensaje_error       TEXT;

-- Soporta ReconciliacionPayPalScheduler: filas 'Pendiente' no actualizadas en
-- los ultimos N minutos (umbral configurable). Se indexa fecha_actualizacion,
-- no fecha_creacion: crearOrdenPayPal reutiliza la misma fila en cada
-- reintento del cliente, así que lo relevante es la edad del ULTIMO intento.
CREATE INDEX IF NOT EXISTS idx_pagos_garantia_estado_actualizacion
    ON pagos_garantia (estado_fondos, fecha_actualizacion);

-- Nuevos valores de estado_fondos ('Reembolsado', 'ReembolsoFallido'): la
-- columna ya es VARCHAR(50) libre, sin CHECK, igual que los 3 valores
-- existentes ('Pendiente' / 'Retenido' / 'Liberado') -- no requieren ALTER.
