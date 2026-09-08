-- =============================================================================
-- V43: indices de clave foranea ausentes en tablas de portafolio, catalogo
-- de servicios, tickets y pagos (mismo patron y motivacion que V24, extendido
-- a tablas que quedaron fuera de aquella revision).
-- =============================================================================
-- PostgreSQL no crea indice automatico para una FK. Verificado con
-- EXPLAIN ANALYZE contra una base con volumen real (~1.4M filas de prueba,
-- ver database/seed-millon.sql): las tablas de abajo solo tenian el indice
-- de su propia PK -- cualquier busqueda por la FK que su metodo de
-- repositorio usa degradaba a Seq Scan completo. Confirmado en vivo, ej.:
--   transacciones_pago WHERE id_pago = ?      -> Seq Scan, 80008 filas descartadas (~5ms)
--   pagos_garantia WHERE id_orden_paypal = ?  -> Seq Scan, 80004 filas descartadas (~14ms)
--     (este ultimo esta en el camino del webhook publico de PayPal, RNF-14:
--      cada notificacion entrante paga hoy un recorrido completo de la tabla)
-- =============================================================================

-- PortafolioItemRepository.findByPortafolioIdPortafolioOrderByFechaSubidaDesc
-- y countByPortafolioIdPortafolio (listado del portafolio de un creador).
CREATE INDEX IF NOT EXISTS idx_portafolio_items_id_portafolio
    ON portafolio_items (id_portafolio, fecha_subida DESC);

-- TicketRevisionRepository.findByPedidoIdPedidoOrderByIdTicketDesc y
-- countByPedidoIdPedido.
CREATE INDEX IF NOT EXISTS idx_tickets_revision_id_pedido
    ON tickets_revision (id_pedido);

-- ComentarioPortafolioRepository.findByItemPortafolioIdItemPortafolioAndEstadoModeracion
-- (listado publico paginado) y countByItemPortafolioIdItemPortafolioAndEstadoModeracion.
CREATE INDEX IF NOT EXISTS idx_comentarios_portafolio_item_estado
    ON comentarios_portafolio (id_item_portafolio, estado_moderacion);

-- TransaccionPagoRepository.findByPagoIdPagoOrderByFechaEjecucionDesc.
CREATE INDEX IF NOT EXISTS idx_transacciones_pago_id_pago
    ON transacciones_pago (id_pago, fecha_ejecucion DESC);

-- ServicioEtiquetaRepository.findByServicioIdServicio(In) (etiquetas de un servicio).
CREATE INDEX IF NOT EXISTS idx_servicio_etiquetas_id_servicio
    ON servicio_etiquetas (id_servicio);

-- ServicioAtributoRepository.findByServicioIdServicio y countByServicioIdServicio.
CREATE INDEX IF NOT EXISTS idx_servicio_atributos_id_servicio
    ON servicio_atributos (id_servicio);

-- PagoGarantiaRepository.findByIdOrdenPaypal: unico punto de entrada del
-- webhook publico de PayPal (RNF-14) para localizar el pago a confirmar.
CREATE INDEX IF NOT EXISTS idx_pagos_garantia_id_orden_paypal
    ON pagos_garantia (id_orden_paypal);
