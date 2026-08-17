-- =============================================================================
-- fn_generar_codigo_pedido
-- Categoria funcional: generacion de codigos secuenciales   Requisito: REQ-F-018
-- =============================================================================
-- Genera y asigna el codigo publico de un pedido con el formato
--
--     ART-<AAAA>-<NNNNNN>        p. ej.  ART-2026-000042
--
-- donde <AAAA> es el ano de creacion del pedido y <NNNNNN> un correlativo de
-- seis digitos tomado de la secuencia seq_codigo_pedido.
--
-- Por que una secuencia y no MAX(codigo)+1: la secuencia entrega valores
-- unicos sin bloquear la tabla y sin sufrir condiciones de carrera cuando dos
-- pedidos se crean a la vez. Es la razon por la que esta operacion no puede
-- resolverse con un contador en Java: dos instancias del backend generarian el
-- mismo numero. La contrapartida aceptada es que los huecos en la secuencia son
-- posibles tras un rollback; el codigo es un identificador, no un contador de
-- pedidos, y su unicidad la garantiza ademas la restriccion UNIQUE de la tabla.
--
-- La rutina es idempotente: si el pedido ya tiene codigo asignado lo devuelve
-- sin consumir un valor nuevo de la secuencia.
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE. El operador
-- || construye el texto del codigo a partir de valores ya tipados, no SQL.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_generar_codigo_pedido(
    p_id_pedido BIGINT
)
RETURNS VARCHAR(20)
LANGUAGE plpgsql
AS $$
DECLARE
    v_codigo_existente VARCHAR(20);
    v_anio             TEXT;
    v_correlativo      BIGINT;
    v_codigo           VARCHAR(20);
BEGIN
    IF p_id_pedido IS NULL THEN
        RAISE EXCEPTION 'fn_generar_codigo_pedido: p_id_pedido es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    SELECT p.codigo_pedido,
           to_char(COALESCE(p.fecha_inicio, CURRENT_TIMESTAMP), 'YYYY')
      INTO v_codigo_existente, v_anio
      FROM pedidos p
     WHERE p.id_pedido = p_id_pedido
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El pedido % no existe', p_id_pedido
            USING ERRCODE = '23503';
    END IF;

    -- Idempotencia: no se consume secuencia si el codigo ya esta asignado.
    IF v_codigo_existente IS NOT NULL THEN
        RETURN v_codigo_existente;
    END IF;

    v_correlativo := nextval('seq_codigo_pedido');
    v_codigo      := 'ART-' || v_anio || '-' || lpad(v_correlativo::TEXT, 6, '0');

    UPDATE pedidos
       SET codigo_pedido = v_codigo
     WHERE id_pedido = p_id_pedido;

    RETURN v_codigo;
END;
$$;

COMMENT ON FUNCTION fn_generar_codigo_pedido(BIGINT)
    IS 'REQ-F-018 - Generacion de codigo secuencial ART-AAAA-NNNNNN para un pedido. Idempotente: devuelve el codigo ya asignado si existe.';
