-- =============================================================================
-- fn_es_seguidor
-- Categoria funcional: consultas multi-tabla / validaciones
-- =============================================================================
-- Retorna TRUE si el usuario especificado sigue al perfil de creador.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_es_seguidor(
    p_id_usuario_seguidor BIGINT,
    p_id_perfil_creador BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    IF p_id_usuario_seguidor IS NULL OR p_id_perfil_creador IS NULL THEN
        RETURN FALSE;
    END IF;

    RETURN EXISTS (
        SELECT 1
          FROM seguidores
         WHERE id_usuario_seguidor = p_id_usuario_seguidor
           AND id_perfil_creador = p_id_perfil_creador
    );
END;
$$;

COMMENT ON FUNCTION fn_es_seguidor(BIGINT, BIGINT)
    IS 'Verifica si un usuario dado sigue a un perfil de creador determinado.';
