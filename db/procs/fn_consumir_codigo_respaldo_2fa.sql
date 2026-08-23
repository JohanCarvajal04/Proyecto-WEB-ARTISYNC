-- =============================================================================
-- fn_consumir_codigo_respaldo_2fa
-- Categoria funcional: validaciones cruzadas                    Requisito: REQ-NF (concurrencia)
-- Fase 1 de docs/basedatos/PLAN-CONCURRENCIA-SP.md — corrige la anomalia A1.
-- =============================================================================
-- Marca un codigo de respaldo 2FA como usado, si y solo si sigue sin usar.
--
-- Sustituye a TwoFactorServiceImpl.validarCodigoOBackup() en la rama de
-- codigos de respaldo, que hacia: SELECT de todos los codigos no usados del
-- usuario -> comparar el hash en un bucle Java -> UPDATE del que coincide.
-- Ese patron read-modify-write NO es atomico: dos peticiones concurrentes con
-- el mismo codigo leen ambas usado = FALSE antes de que ninguna escriba, y
-- ambas terminan devolviendo TRUE. Un codigo de un solo uso quedaba
-- consumible dos veces (actualizacion perdida) -- un bypass de segundo factor.
--
-- Por que esta forma lo corrige: es una UNICA sentencia UPDATE con el propio
-- "usado = FALSE" como predicado. Bajo READ COMMITTED (el nivel del proyecto,
-- ver docs/basedatos/PLAN-CONCURRENCIA-SP.md §0.4), cuando un UPDATE encuentra
-- una fila que otra transaccion ya modifico y confirmo, PostgreSQL re-evalua
-- el WHERE sobre esa version nueva (EvalPlanQual) antes de aplicar el cambio.
-- Si la primera transaccion ya puso usado = TRUE, la segunda ve el predicado
-- fallar y no actualiza nada: no hace falta SELECT ... FOR UPDATE previo, el
-- propio UPDATE toma el bloqueo de fila y la re-evaluacion cierra la ventana.
--
-- Devuelve TRUE si este codigo (usuario + hash) existia y no estaba usado
-- -- exactamente uno de los llamantes concurrentes lo recibe --, FALSE en
-- cualquier otro caso (no existe, o ya estaba usado).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE. Solo
-- ve el hash SHA-256 del codigo, nunca el codigo en texto plano (se calcula
-- en Java antes de invocar esta funcion, igual que en fn_restablecer_contrasena).
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_consumir_codigo_respaldo_2fa(
    p_id_usuario  BIGINT,
    p_codigo_hash VARCHAR(255)
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_codigo BIGINT;
BEGIN
    IF p_id_usuario IS NULL OR p_codigo_hash IS NULL THEN
        RAISE EXCEPTION 'fn_consumir_codigo_respaldo_2fa: p_id_usuario y p_codigo_hash son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    UPDATE codigos_respaldo_2fa
       SET usado = TRUE
     WHERE id_usuario = p_id_usuario
       AND codigo_hash = p_codigo_hash
       AND usado = FALSE
    RETURNING id_codigo INTO v_id_codigo;

    RETURN v_id_codigo IS NOT NULL;
END;
$$;

COMMENT ON FUNCTION fn_consumir_codigo_respaldo_2fa(BIGINT, VARCHAR)
    IS 'Fase 1 concurrencia - UPDATE atomico que consume un codigo de respaldo 2FA una sola vez, eliminando la actualizacion perdida del patron read-modify-write anterior.';
