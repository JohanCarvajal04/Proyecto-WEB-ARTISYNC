-- =============================================================================
-- V47: pago de tickets de revision que superan el limite del contrato, y
-- rechazo automatico a las 48h si ese pago no se confirma (REQ-F-022b/c).
-- =============================================================================

-- Necesaria para que TicketRevisionExpiracionScheduler pueda calcular "48
-- horas desde la creacion del ticket"; no existia ninguna columna de fecha.
ALTER TABLE tickets_revision
    ADD COLUMN IF NOT EXISTS fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_tickets_revision_estado_fecha
    ON tickets_revision (estado_ticket, fecha_creacion);

-- Tabla separada de pagos_garantia a proposito: id_contrato de pagos_garantia
-- es UNIQUE (un solo pago de garantia por contrato, para siempre); el cargo
-- de un ticket de revision es un cobro independiente que no debe competir
-- por esa fila ni arriesgar el flujo de escrow principal ya verificado
-- (REQ-F-020 / REQ-NF-014 / REQ-NF-019).
CREATE TABLE IF NOT EXISTS pagos_ticket_revision (
    id_pago_ticket       BIGSERIAL PRIMARY KEY,
    id_ticket            BIGINT NOT NULL UNIQUE REFERENCES tickets_revision(id_ticket) ON DELETE CASCADE,
    id_orden_paypal      VARCHAR(100),
    url_aprobacion       VARCHAR(255),
    monto                NUMERIC(10,2) NOT NULL,
    -- 'Pendiente' | 'Pagado' | 'Expirado'
    estado_pago          VARCHAR(50) NOT NULL DEFAULT 'Pendiente',
    mensaje_error        TEXT,
    fecha_creacion       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pagos_ticket_revision_id_orden_paypal
    ON pagos_ticket_revision (id_orden_paypal);
