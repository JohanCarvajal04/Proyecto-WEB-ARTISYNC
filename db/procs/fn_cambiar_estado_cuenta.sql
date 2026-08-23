-- =============================================================================
-- fn_cambiar_estado_cuenta
-- Categoria funcional: validaciones cruzadas                    Requisito: REQ-NF (concurrencia)
-- Fase 1 de docs/basedatos/PLAN-CONCURRENCIA-SP.md — corrige la anomalia A6 (compuesta).
-- =============================================================================
-- Cambia estado_cuenta de un usuario y, solo si la cuenta pasa de activa a
-- inactiva, revoca sus sesiones (fn_revocar_sesiones_usuario) en la MISMA
-- transaccion. Unifica el par "cambiar estado + revocar sesiones" que hoy se
-- repite, con ligeras variaciones, en:
--   - AdminUserServiceImpl.changeEstado
--   - AdminUserServiceImpl.deleteUser (soft-delete: estadoCuenta = false)
--   - UserServiceImpl.deleteOwnAccount (idem)
--   - la rama de estadoCuenta dentro de AdminUserServiceImpl.updateUser
--
-- Anomalia que corrige: sin SELECT ... FOR UPDATE, dos administradores
-- operando sobre el mismo usuario a la vez (uno reactivandolo, otro
-- desactivandolo) pueden pisarse: el que desactiva puede leer un
-- estado_cuenta ya obsoleto y decidir, incorrectamente, NO revocar sesiones
-- -- exactamente el caso en que mas importa hacerlo. El FOR UPDATE serializa
-- ambas operaciones: la segunda espera a que la primera confirme y lee su
-- resultado real, nunca un valor a medias (actualizacion perdida cerrada).
--
-- Devuelve las filas de fn_revocar_sesiones_usuario (jti + segundos
-- restantes) cuando hubo transicion activa->inactiva, o un conjunto vacio si
-- no la hubo (incluye reactivar una cuenta, o "cambiar" a un estado igual al
-- actual).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_cambiar_estado_cuenta(
    p_id_usuario BIGINT,
    p_estado     BOOLEAN
)
RETURNS TABLE (jti VARCHAR(36), segundos_restantes INTEGER)
LANGUAGE plpgsql
AS $$
DECLARE
    v_estado_anterior BOOLEAN;
BEGIN
    IF p_id_usuario IS NULL OR p_estado IS NULL THEN
        RAISE EXCEPTION 'fn_cambiar_estado_cuenta: p_id_usuario y p_estado son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    -- FOR UPDATE: serializa dos cambios de estado concurrentes del mismo
    -- usuario. Sin el, el segundo escritor podria leer un estado_anterior ya
    -- superado por el primero y decidir mal si corresponde revocar sesiones.
    SELECT estado_cuenta INTO v_estado_anterior
      FROM usuarios
     WHERE id_usuario = p_id_usuario
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario no encontrado con ID: %', p_id_usuario
            USING ERRCODE = 'P0002';
    END IF;

    UPDATE usuarios
       SET estado_cuenta = p_estado
     WHERE id_usuario = p_id_usuario;

    IF v_estado_anterior AND NOT p_estado THEN
        RETURN QUERY SELECT * FROM fn_revocar_sesiones_usuario(p_id_usuario);
    END IF;

    RETURN;
END;
$$;

COMMENT ON FUNCTION fn_cambiar_estado_cuenta(BIGINT, BOOLEAN)
    IS 'Fase 1 concurrencia - Cambia estado_cuenta y revoca sesiones (transicion activa->inactiva) atomicamente bajo SELECT FOR UPDATE, unificando el patron repetido en changeEstado/deleteUser/deleteOwnAccount.';
