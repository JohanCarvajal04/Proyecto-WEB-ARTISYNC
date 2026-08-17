-- =============================================================================
-- fn_cerrar_pedidos_vencidos
-- Categoria funcional: actualizaciones masivas      Requisito: REQ-F-019
-- =============================================================================
-- Cierra en bloque los pedidos cuya fecha de entrega estimada ya vencio y que
-- todavia no alcanzaron una etapa final de su flujo de trabajo. El cierre se
-- materializa como una transicion nueva en historial_estados_pedido hacia la
-- etapa final configurada para el flujo del pedido (flujo_etapas_config con
-- es_etapa_final = TRUE y el numero_orden mas alto).
--
-- El modelo de datos de Artisync no guarda el estado en la tabla pedidos: el
-- estado vigente es la ultima fila de historial_estados_pedido. Por eso el
-- cierre es un INSERT y no un UPDATE, y por eso la rutina es idempotente: un
-- pedido que ya esta en etapa final queda excluido por el predicado.
--
-- Ejecutar esta operacion como una sola sentencia en el motor, en lugar de
-- iterar en Java pedido por pedido, evita el patron N+1 que tenia
-- PedidoServicioImpl y garantiza que todas las transiciones compartan la misma
-- transaccion y la misma marca de tiempo.
--
-- Devuelve el numero de pedidos cerrados.
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_cerrar_pedidos_vencidos(
    p_dias_gracia INTEGER DEFAULT 0
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_gracia   INTEGER;
    v_limite   TIMESTAMP;
    v_cerrados INTEGER := 0;
BEGIN
    -- Un periodo de gracia negativo cerraria pedidos aun vigentes.
    v_gracia := GREATEST(COALESCE(p_dias_gracia, 0), 0);
    v_limite := CURRENT_TIMESTAMP - (v_gracia * INTERVAL '1 day');

    WITH etapa_final_por_flujo AS (
        -- Etapa final de cada flujo. DISTINCT ON toma la de mayor numero_orden
        -- cuando un flujo declara mas de una etapa terminal.
        SELECT DISTINCT ON (fec.id_flujo)
               fec.id_flujo,
               fec.id_etapa
          FROM flujo_etapas_config fec
         WHERE fec.es_etapa_final IS TRUE
         ORDER BY fec.id_flujo, fec.numero_orden DESC
    ),
    ultimo_estado AS (
        SELECT DISTINCT ON (h.id_pedido)
               h.id_pedido,
               h.id_etapa
          FROM historial_estados_pedido h
         ORDER BY h.id_pedido, h.fecha_transicion DESC, h.id_historial_estado DESC
    ),
    vencidos AS (
        SELECT p.id_pedido,
               ef.id_etapa
          FROM pedidos p
          JOIN etapa_final_por_flujo ef ON ef.id_flujo = p.id_flujo
          LEFT JOIN ultimo_estado ue    ON ue.id_pedido = p.id_pedido
         WHERE p.fecha_entrega_estimada IS NOT NULL
           AND p.fecha_entrega_estimada < v_limite
           -- Excluye los que ya estan en una etapa final (idempotencia).
           AND (ue.id_etapa IS NULL OR ue.id_etapa <> ef.id_etapa)
    )
    INSERT INTO historial_estados_pedido (id_pedido, id_etapa, fecha_transicion, observacion)
    SELECT v.id_pedido,
           v.id_etapa,
           CURRENT_TIMESTAMP,
           'Cierre automatico por vencimiento de la fecha de entrega estimada '
           || '(fn_cerrar_pedidos_vencidos, dias de gracia: ' || v_gracia || ')'
      FROM vencidos v;

    GET DIAGNOSTICS v_cerrados = ROW_COUNT;
    RETURN v_cerrados;
END;
$$;

COMMENT ON FUNCTION fn_cerrar_pedidos_vencidos(INTEGER)
    IS 'REQ-F-019 - Actualizacion masiva: cierra los pedidos vencidos insertando su transicion a etapa final. Idempotente. Devuelve el numero de pedidos cerrados.';
