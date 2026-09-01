-- =============================================================================
-- fn_desactivar_2fa
-- Categoria funcional: validaciones cruzadas + escritura multi-tabla   Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §7 — corrige la anomalia A4.
-- =============================================================================
-- Desactiva el 2FA de un usuario y purga sus codigos de respaldo, en una
-- unica transaccion. Contraparte de fn_configurar_2fa; unifica el codigo que
-- antes estaba DUPLICADO entre dos sitios que hacian exactamente lo mismo:
--   - TwoFactorServiceImpl.disable2Fa (con el codigo/TOTP ya validado)
--   - AdminUserServiceImpl.updateUser, rama dosFactoresHabilitado = false
--     (el administrador fuerza la desactivacion sin validar codigo)
-- Ambos hacian: findByUsuarioIdUsuario + set esta_habilitado=false + save +
-- deleteByUsuarioIdUsuario en Java, sin atomicidad entre el UPDATE y el DELETE.
--
-- Es intencionalmente IDEMPOTENTE y silenciosa si el usuario no tiene 2FA
-- configurado: devuelve FALSE en vez de lanzar excepcion, porque el caso de
-- uso administrativo (forzar 2FA=false) es legitimo aunque el usuario nunca
-- lo haya configurado -- exactamente el comportamiento que ya tenia el
-- `ifPresent(...)` de AdminUserServiceImpl.updateUser.
--
-- Devuelve TRUE si habia un registro de 2FA y quedo desactivado, FALSE si el
-- usuario no tenia 2FA configurado (no-op).
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_desactivar_2fa(
    p_id_usuario BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_afectadas INTEGER;
BEGIN
    IF p_id_usuario IS NULL THEN
        RAISE EXCEPTION 'fn_desactivar_2fa: p_id_usuario es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    UPDATE autenticacion_dos_factores
       SET esta_habilitado = FALSE
     WHERE id_usuario = p_id_usuario;

    GET DIAGNOSTICS v_afectadas = ROW_COUNT;

    -- Purga de codigos de respaldo en la misma transaccion que el UPDATE: no
    -- hay ventana en la que el 2FA aparezca desactivado pero sus codigos de
    -- respaldo sigan siendo validos (o viceversa).
    DELETE FROM codigos_respaldo_2fa WHERE id_usuario = p_id_usuario;

    RETURN v_afectadas > 0;
END;
$$;

COMMENT ON FUNCTION fn_desactivar_2fa(BIGINT)
    IS 'Fase 3 concurrencia - Desactiva 2FA y purga codigos de respaldo atomicamente; idempotente si el usuario no tenia 2FA configurado. Unifica el codigo duplicado entre TwoFactorServiceImpl.disable2Fa y AdminUserServiceImpl.updateUser.';
