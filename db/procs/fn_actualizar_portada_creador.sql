-- =============================================================================
-- fn_actualizar_portada_creador
-- Categoria funcional: actualizaciones
-- =============================================================================
-- Actualiza la URL de portada y el titulo profesional de un perfil de creador.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_actualizar_portada_creador(
    p_id_perfil BIGINT,
    p_url_portada VARCHAR(500),
    p_titulo_profesional VARCHAR(150)
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_id_perfil IS NULL THEN
        RAISE EXCEPTION 'El id de perfil es obligatorio';
    END IF;

    UPDATE perfiles_creadores
       SET url_portada = COALESCE(p_url_portada, url_portada),
           titulo_profesional = COALESCE(p_titulo_profesional, titulo_profesional)
     WHERE id_perfil = p_id_perfil;

    RETURN FOUND;
END;
$$;

COMMENT ON FUNCTION fn_actualizar_portada_creador(BIGINT, VARCHAR, VARCHAR)
    IS 'Actualiza la imagen de portada y especialidad profesional de un perfil de creador.';
