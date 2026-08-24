-- =============================================================================
-- fn_cambiar_contrasena
-- Categoria funcional: validaciones cruzadas                    Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §6 — corrige la anomalia A7.
-- =============================================================================
-- Aplica un cambio de contrasena de forma condicionada: solo si el hash
-- almacenado sigue siendo EXACTAMENTE el que Java verifico con BCrypt antes
-- de invocar esta funcion (compare-and-swap).
--
-- Sustituye a UserServiceImpl.changePassword (parte de escritura), que hacia
-- un UPDATE incondicional tras la verificacion: usuario.setContrasenaHash(...)
-- + save(). Si dos peticiones concurrentes cambiaban la contrasena del mismo
-- usuario (dos pestanas, un cliente reintentando tras un timeout aparente),
-- la segunda en escribir pisaba silenciosamente el resultado de la primera --
-- ninguna de las dos se enteraba de que "gano" la otra (actualizacion perdida).
--
-- BCrypt en si permanece fuera del motor (la comparacion de la contrasena
-- ACTUAL contra el hash se sigue haciendo en Java, con passwordEncoder.matches,
-- antes de invocar esta funcion): lo que se traslada al motor es la ESCRITURA
-- condicionada, usando el propio hash verificado como testigo de version. Si
-- el hash cambio entre la verificacion en Java y este UPDATE, el predicado
-- "contrasena_hash = p_hash_esperado" no coincide y la fila no se actualiza
-- (0 filas afectadas), sin necesidad de un SELECT ... FOR UPDATE previo.
--
-- Lanza excepcion (ERRCODE 40001, serialization_failure: el codigo estandar
-- de PostgreSQL para "otra transaccion se te adelanto") si 0 filas resultaron
-- afectadas, para que la capa Java pueda distinguir "contrasena actual
-- incorrecta" (validado antes, en Java) de "alguien mas cambio la contrasena
-- justo ahora" (aqui).
--
-- Devuelve TRUE si el cambio se aplico.
--
-- Seguridad: parametros formales tipados (los hashes BCrypt, nunca la
-- contrasena en texto plano); sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_cambiar_contrasena(
    p_id_usuario    BIGINT,
    p_hash_esperado VARCHAR(255),
    p_hash_nuevo    VARCHAR(255)
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_afectadas INTEGER;
BEGIN
    IF p_id_usuario IS NULL OR p_hash_esperado IS NULL OR p_hash_nuevo IS NULL THEN
        RAISE EXCEPTION 'fn_cambiar_contrasena: todos los parametros son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    -- El predicado contrasena_hash = p_hash_esperado es el compare-and-swap:
    -- bajo READ COMMITTED, si otra transaccion ya cambio la contrasena y
    -- confirmo, PostgreSQL re-evalua este WHERE sobre esa version nueva
    -- (EvalPlanQual) y el predicado deja de cumplirse -- ROW_COUNT queda en 0
    -- sin que ninguna de las dos escrituras se pierda en silencio.
    UPDATE usuarios
       SET contrasena_hash = p_hash_nuevo
     WHERE id_usuario = p_id_usuario
       AND contrasena_hash = p_hash_esperado;

    GET DIAGNOSTICS v_afectadas = ROW_COUNT;

    IF v_afectadas = 0 THEN
        RAISE EXCEPTION 'La contrasena fue modificada por otra sesion. Vuelve a intentarlo.'
            USING ERRCODE = '40001';
    END IF;

    RETURN TRUE;
END;
$$;

COMMENT ON FUNCTION fn_cambiar_contrasena(BIGINT, VARCHAR, VARCHAR)
    IS 'Fase 3 concurrencia - UPDATE condicionado (compare-and-swap sobre el hash) que aplica un cambio de contrasena solo si nadie mas la cambio primero, eliminando la actualizacion perdida (A7).';
