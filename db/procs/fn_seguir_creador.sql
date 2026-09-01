-- =============================================================================
-- fn_seguir_creador
-- Categoria funcional: validaciones cruzadas + escritura multi-tabla
-- =============================================================================
-- Permite a un usuario autenticado seguir a un perfil de creador.
-- Valida que el perfil exista y que el usuario no sea el propietario del perfil.
-- Es idempotente (ON CONFLICT DO NOTHING).
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_seguir_creador(
    p_id_usuario_seguidor BIGINT,
    p_id_perfil_creador BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_usuario_creador BIGINT;
BEGIN
    IF p_id_usuario_seguidor IS NULL OR p_id_perfil_creador IS NULL THEN
        RAISE EXCEPTION 'Los parametros usuario seguidor y perfil creador son obligatorios';
    END IF;

    SELECT id_usuario INTO v_id_usuario_creador
      FROM perfiles_creadores
     WHERE id_perfil = p_id_perfil_creador;

    IF v_id_usuario_creador IS NULL THEN
        RAISE EXCEPTION 'El perfil de creador especificado no existe';
    END IF;

    IF v_id_usuario_creador = p_id_usuario_seguidor THEN
        RAISE EXCEPTION 'Un creador no puede seguirse a si mismo';
    END IF;

    INSERT INTO seguidores (id_usuario_seguidor, id_perfil_creador, fecha_seguimiento, notificaciones_activas)
    VALUES (p_id_usuario_seguidor, p_id_perfil_creador, NOW(), TRUE)
    ON CONFLICT (id_usuario_seguidor, id_perfil_creador) DO NOTHING;

    RETURN TRUE;
END;
$$;

COMMENT ON FUNCTION fn_seguir_creador(BIGINT, BIGINT)
    IS 'Registra un seguimiento de usuario a creador validando no auto-seguimiento.';
