-- =============================================================================
-- fn_dejar_de_seguir_creador
-- Categoria funcional: actualizaciones masivas / eliminacion
-- =============================================================================
-- Permite a un usuario dejar de seguir a un perfil de creador.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_dejar_de_seguir_creador(
    p_id_usuario_seguidor BIGINT,
    p_id_perfil_creador BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_id_usuario_seguidor IS NULL OR p_id_perfil_creador IS NULL THEN
        RETURN FALSE;
    END IF;

    DELETE FROM seguidores
     WHERE id_usuario_seguidor = p_id_usuario_seguidor
       AND id_perfil_creador = p_id_perfil_creador;

    RETURN TRUE;
END;
$$;

COMMENT ON FUNCTION fn_dejar_de_seguir_creador(BIGINT, BIGINT)
    IS 'Elimina la relacion de seguimiento entre un usuario y un perfil de creador.';
