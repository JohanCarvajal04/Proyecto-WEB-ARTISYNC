-- =============================================================================
-- fn_conteo_seguidores
-- Categoria funcional: calculos agregados
-- =============================================================================
-- Retorna el total de seguidores acumulados por un perfil de creador.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_conteo_seguidores(
    p_id_perfil_creador BIGINT
)
RETURNS BIGINT
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    IF p_id_perfil_creador IS NULL THEN
        RETURN 0;
    END IF;

    RETURN (
        SELECT COUNT(*)
          FROM seguidores
         WHERE id_perfil_creador = p_id_perfil_creador
    );
END;
$$;

COMMENT ON FUNCTION fn_conteo_seguidores(BIGINT)
    IS 'Calcula el numero total de seguidores de un perfil de creador.';
