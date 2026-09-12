-- =============================================================================
-- V49: boceto con marca de agua por pedido, y bandera requiere_boceto por etapa.
-- =============================================================================
-- Mismo patron que entregables_finales: una fila por pedido que se resube y
-- reemplaza (sin historial de versiones anteriores). El creador aplica la
-- marca de agua el mismo fuera de la plataforma y sube la imagen ya marcada;
-- el cliente solo la visualiza, no hay aprobacion de boceto (eso sigue yendo
-- por el chat del pedido).
--
-- requiere_boceto sigue el mismo criterio que ya acepta requiere_entregable:
-- un flag por combinacion flujo+etapa, sin ligar el boceto en si a una etapa
-- concreta -- si un mismo flujo tuviera dos etapas que lo exigieran, basta
-- con un boceto subido en cualquiera de ellas.
-- =============================================================================

CREATE TABLE IF NOT EXISTS bocetos (
    id_boceto BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    url_imagen VARCHAR(255) NOT NULL,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- PostgreSQL no indexa una FK automaticamente (mismo motivo documentado en
-- V43 para portafolio/pagos); BocetoRepository consulta siempre por id_pedido.
CREATE INDEX IF NOT EXISTS idx_bocetos_id_pedido ON bocetos (id_pedido);

ALTER TABLE flujo_etapas_config
    ADD COLUMN IF NOT EXISTS requiere_boceto BOOLEAN NOT NULL DEFAULT FALSE;
