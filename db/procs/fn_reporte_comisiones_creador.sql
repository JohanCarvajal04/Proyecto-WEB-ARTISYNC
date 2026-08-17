-- =============================================================================
-- fn_reporte_comisiones_creador
-- Categoria funcional: reportes                     Requisito: REQ-NF-013
-- =============================================================================
-- Produce el reporte financiero de un creador en una ventana temporal: importe
-- bruto liberado, comision de la plataforma, importe neto, numero de pedidos y
-- desglose por transaccion. Recorre la cadena completa del patron escrow:
--   transacciones_pago -> pagos_garantia -> contratos -> pedidos -> servicios
--
-- Sustituye a la consulta JPQL de tres JOIN de
--   repository/legal/TransaccionPagoRepository.findByCreadorPerfilId
-- que devolvia entidades crudas y obligaba a agregar en Java.
--
-- La comision de la plataforma se parametriza (p_tasa_comision) en lugar de
-- fijarse en la rutina, para que el reporte pueda recalcularse historicamente
-- si la tasa cambia sin necesidad de versionar una nueva funcion.
--
-- Contrato de tipo de retorno: JSONB (ver db/procs/README.md).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_reporte_comisiones_creador(
    p_id_perfil     BIGINT,
    p_fecha_desde   TIMESTAMP     DEFAULT NULL,
    p_fecha_hasta   TIMESTAMP     DEFAULT NULL,
    p_tasa_comision NUMERIC(5,4)  DEFAULT 0.1000
)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_tasa       NUMERIC(5,4);
    v_bruto      NUMERIC(12,2) := 0;
    v_pedidos    BIGINT        := 0;
    v_operaciones BIGINT       := 0;
    v_detalle    JSONB;
BEGIN
    IF p_id_perfil IS NULL THEN
        RAISE EXCEPTION 'fn_reporte_comisiones_creador: p_id_perfil es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    -- La tasa se acota al rango [0,1]: una tasa fuera de rango produciria un
    -- neto negativo o superior al bruto, y el reporte alimenta el capitulo de
    -- evaluacion empirica del documento academico.
    v_tasa := LEAST(GREATEST(COALESCE(p_tasa_comision, 0.1000), 0::NUMERIC), 1::NUMERIC);

    WITH movimientos AS (
        SELECT t.id_transaccion,
               t.tipo_transaccion,
               t.monto,
               t.fecha_ejecucion,
               p.id_pedido,
               s.titulo_servicio
          FROM transacciones_pago t
          JOIN pagos_garantia pg ON pg.id_pago     = t.id_pago
          JOIN contratos      c  ON c.id_contrato  = pg.id_contrato
          JOIN pedidos        p  ON p.id_pedido    = c.id_pedido
          JOIN servicios      s  ON s.id_servicio  = p.id_servicio
         WHERE s.id_perfil = p_id_perfil
           AND (p_fecha_desde IS NULL OR t.fecha_ejecucion >= p_fecha_desde)
           AND (p_fecha_hasta IS NULL OR t.fecha_ejecucion <= p_fecha_hasta)
    )
    SELECT COALESCE(SUM(m.monto), 0),
           COUNT(DISTINCT m.id_pedido),
           COUNT(*),
           COALESCE(
               jsonb_agg(
                   jsonb_build_object(
                       'idTransaccion',  m.id_transaccion,
                       'idPedido',       m.id_pedido,
                       'servicio',       m.titulo_servicio,
                       'tipo',           m.tipo_transaccion,
                       'monto',          m.monto,
                       'fechaEjecucion', to_char(m.fecha_ejecucion, 'YYYY-MM-DD"T"HH24:MI:SS')
                   )
                   ORDER BY m.fecha_ejecucion DESC, m.id_transaccion DESC
               ),
               '[]'::JSONB)
      INTO v_bruto, v_pedidos, v_operaciones, v_detalle
      FROM movimientos m;

    RETURN jsonb_build_object(
        'idPerfil',      p_id_perfil,
        'fechaDesde',    to_char(p_fecha_desde, 'YYYY-MM-DD"T"HH24:MI:SS'),
        'fechaHasta',    to_char(p_fecha_hasta, 'YYYY-MM-DD"T"HH24:MI:SS'),
        'tasaComision',  v_tasa,
        'totalPedidos',  v_pedidos,
        'totalOperaciones', v_operaciones,
        'montoBruto',    ROUND(v_bruto, 2),
        'comision',      ROUND(v_bruto * v_tasa, 2),
        'montoNeto',     ROUND(v_bruto - (v_bruto * v_tasa), 2),
        'detalle',       COALESCE(v_detalle, '[]'::JSONB)
    );
END;
$$;

COMMENT ON FUNCTION fn_reporte_comisiones_creador(BIGINT, TIMESTAMP, TIMESTAMP, NUMERIC)
    IS 'REQ-NF-013 - Reporte financiero por creador (bruto, comision, neto y detalle) sobre la cadena escrow.';
