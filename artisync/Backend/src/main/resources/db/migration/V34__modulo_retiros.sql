-- ==============================================================================
-- MIGRACIÓN V34: MÓDULO DE RETIROS (PAYOUTS PARA CREADORES)
-- ==============================================================================
-- Hasta ahora, cuando el cliente aprobaba una entrega, el sistema solo
-- registraba en transacciones_pago un "Egreso" contable a favor del creador
-- (EntregableServicioImpl.aprobarEntrega): el dinero real quedaba en la
-- cuenta PayPal Business de la plataforma, sin ningún mecanismo para que el
-- creador lo solicitara o recibiera. Estas dos tablas cierran ese ciclo.

-- 1. Datos de cobro del creador (correo de PayPal para recibir payouts).
-- Tabla separada de perfiles_creadores a propósito: ese perfil es público
-- (GET /api/v1/perfiles/*, sin auth), así que un dato de cobro no debe vivir
-- ahí por el riesgo de que un futuro DTO lo exponga por descuido.
CREATE TABLE datos_pago_creador (
    id_datos_pago       BIGSERIAL PRIMARY KEY,
    id_usuario          BIGINT NOT NULL UNIQUE REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    correo_paypal       VARCHAR(150) NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Solicitudes de retiro.
CREATE TABLE solicitudes_retiro (
    id_solicitud            BIGSERIAL PRIMARY KEY,
    id_usuario_creador      BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    monto_solicitado        NUMERIC(10,2) NOT NULL CHECK (monto_solicitado > 0),
    -- Copia inmutable del correo al momento de solicitar: si el creador cambia
    -- su correo de PayPal despues, no debe alterar un retiro ya en curso.
    correo_paypal_destino   VARCHAR(150) NOT NULL,
    -- 'Pendiente' | 'Aprobado' | 'Pagado' | 'Rechazado' | 'Fallido'
    estado                  VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
    id_payout_paypal        VARCHAR(100),
    id_item_payout_paypal   VARCHAR(100),
    nota_admin              TEXT,
    mensaje_error           TEXT,
    fecha_solicitud         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_decision          TIMESTAMP,
    fecha_pago              TIMESTAMP,
    id_admin_decisor        BIGINT REFERENCES usuarios(id_usuario)
);

CREATE INDEX idx_solicitudes_retiro_usuario ON solicitudes_retiro(id_usuario_creador);
CREATE INDEX idx_solicitudes_retiro_estado ON solicitudes_retiro(estado);

-- Como máximo una solicitud "en curso" (Pendiente o Aprobado) por creador a la
-- vez, aplicado a nivel de BD para no depender solo del chequeo en Java (mismo
-- espíritu que V31__fix_race_condicion_decision_verificacion.sql: una
-- condición de carrera entre el SELECT de validación y el INSERT no debe poder
-- colar una segunda solicitud sobre el mismo dinero).
CREATE UNIQUE INDEX uq_solicitud_retiro_pendiente_por_creador
    ON solicitudes_retiro (id_usuario_creador)
    WHERE estado IN ('Pendiente', 'Aprobado');
