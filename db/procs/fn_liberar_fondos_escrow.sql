-- =============================================================================
-- fn_liberar_fondos_escrow
-- Categoria funcional: validaciones cruzadas        Requisito: REQ-F-021
-- =============================================================================
-- Libera los fondos retenidos de un pedido bajo el patron escrow, pero solo
-- despues de comprobar, dentro de la misma transaccion y con el registro
-- bloqueado, las cinco condiciones de negocio que lo autorizan:
--
--   1. El pedido tiene contrato formalizado y pago de garantia asociado.
--   2. Los fondos estan en estado 'Retenido' (no liberados ni reembolsados).
--   3. El contrato esta firmado por ambas partes (cliente y creador).
--   4. Existe al menos un entregable final cargado para el pedido.
--   5. No queda ningun ticket de revision abierto.
--
-- Por que en el motor y no en PagoServicioImpl: las cinco lecturas y las dos
-- escrituras deben ser atomicas frente a otra liberacion concurrente del mismo
-- pedido. La fila de pagos_garantia se toma con SELECT ... FOR UPDATE, de modo
-- que dos llamadas simultaneas se serializan y la segunda observa el estado ya
-- cambiado por la primera. Resolverlo en Java exigiria un bloqueo pesimista
-- explicito y varias idas y vueltas a la base.
--
-- Efectos: actualiza pagos_garantia.estado_fondos a 'Liberado', marca los
-- entregables del pedido como liberados y registra la transaccion contable.
--
-- Devuelve TRUE si libero los fondos, FALSE si no habia nada que liberar
-- (idempotencia: una segunda llamada sobre el mismo pedido devuelve FALSE).
-- Lanza excepcion cuando alguna precondicion de negocio no se cumple, para que
-- la capa de servicio pueda distinguir "ya estaba liberado" de "no se puede".
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_liberar_fondos_escrow(
    p_id_pedido BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_pago         BIGINT;
    v_monto           NUMERIC(10,2);
    v_estado_fondos   VARCHAR(50);
    v_firma_cliente   VARCHAR(255);
    v_firma_creador   VARCHAR(255);
    v_entregables     INTEGER;
    v_tickets_abiertos INTEGER;
BEGIN
    IF p_id_pedido IS NULL THEN
        RAISE EXCEPTION 'fn_liberar_fondos_escrow: p_id_pedido es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    -- (1) Contrato + pago de garantia. FOR UPDATE serializa liberaciones
    -- concurrentes sobre el mismo pedido.
    SELECT pg.id_pago, pg.monto_retenido, pg.estado_fondos,
           c.hash_firma_cliente, c.hash_firma_creador
      INTO v_id_pago, v_monto, v_estado_fondos, v_firma_cliente, v_firma_creador
      FROM pagos_garantia pg
      JOIN contratos c ON c.id_contrato = pg.id_contrato
     WHERE c.id_pedido = p_id_pedido
       FOR UPDATE OF pg;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El pedido % no tiene contrato con pago de garantia asociado', p_id_pedido
            USING ERRCODE = '23503';
    END IF;

    -- (2) Idempotencia: si ya no estan retenidos, no hay nada que hacer.
    IF v_estado_fondos IS DISTINCT FROM 'Retenido' THEN
        RETURN FALSE;
    END IF;

    -- (3) Firma de ambas partes.
    IF v_firma_cliente IS NULL OR v_firma_creador IS NULL THEN
        RAISE EXCEPTION 'El contrato del pedido % no esta firmado por ambas partes', p_id_pedido
            USING ERRCODE = '23514';
    END IF;

    -- (4) Entregable final cargado.
    SELECT COUNT(*) INTO v_entregables
      FROM entregables_finales ef
     WHERE ef.id_pedido = p_id_pedido;

    IF v_entregables = 0 THEN
        RAISE EXCEPTION 'El pedido % no tiene entregables finales cargados', p_id_pedido
            USING ERRCODE = '23514';
    END IF;

    -- (5) Sin revisiones abiertas.
    SELECT COUNT(*) INTO v_tickets_abiertos
      FROM tickets_revision tr
     WHERE tr.id_pedido = p_id_pedido
       AND tr.estado_ticket = 'Abierto';

    IF v_tickets_abiertos > 0 THEN
        RAISE EXCEPTION 'El pedido % tiene % ticket(s) de revision abiertos', p_id_pedido, v_tickets_abiertos
            USING ERRCODE = '23514';
    END IF;

    -- Precondiciones satisfechas: se aplican los tres efectos.
    UPDATE pagos_garantia
       SET estado_fondos = 'Liberado'
     WHERE id_pago = v_id_pago;

    UPDATE entregables_finales
       SET esta_liberado = TRUE
     WHERE id_pedido = p_id_pedido;

    INSERT INTO transacciones_pago (id_pago, tipo_transaccion, monto, fecha_ejecucion)
    VALUES (v_id_pago, 'LIBERACION', v_monto, CURRENT_TIMESTAMP);

    RETURN TRUE;
END;
$$;

COMMENT ON FUNCTION fn_liberar_fondos_escrow(BIGINT)
    IS 'REQ-F-021 - Validacion cruzada: libera los fondos escrow de un pedido tras verificar contrato firmado, entregable cargado y ausencia de revisiones abiertas. TRUE si libero, FALSE si ya estaba liberado.';
