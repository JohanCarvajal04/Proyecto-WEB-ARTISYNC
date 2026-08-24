-- =============================================================================
-- fn_listar_creadores_seguidos_novedades
-- Categoria funcional: consultas multi-tabla / reportes
-- =============================================================================
-- Devuelve los creadores que el usuario sigue junto a su resumen de novedades.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_listar_creadores_seguidos_novedades(
    p_id_usuario_seguidor BIGINT
)
RETURNS TABLE (
    id_perfil BIGINT,
    id_usuario BIGINT,
    nombres_usuario VARCHAR,
    apellidos_usuario VARCHAR,
    handle VARCHAR,
    url_foto_perfil VARCHAR,
    titulo_profesional VARCHAR,
    resumen_novedad TEXT,
    tipo_novedad VARCHAR,
    fecha_novedad TIMESTAMP
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        pc.id_perfil,
        u.id_usuario,
        u.nombres_usuario,
        u.apellidos_usuario,
        COALESCE('@' || LOWER(REPLACE(u.nombres_usuario, ' ', '')), '@creador')::VARCHAR AS handle,
        u.url_foto_perfil,
        pc.titulo_profesional,
        'Actividad reciente en su perfil'::TEXT AS resumen_novedad,
        'GENERAL'::VARCHAR AS tipo_novedad,
        s.fecha_seguimiento::TIMESTAMP AS fecha_novedad
    FROM seguidores s
    JOIN perfiles_creadores pc ON pc.id_perfil = s.id_perfil_creador
    JOIN usuarios u ON u.id_usuario = pc.id_usuario
    WHERE s.id_usuario_seguidor = p_id_usuario_seguidor
    ORDER BY s.fecha_seguimiento DESC;
END;
$$;

COMMENT ON FUNCTION fn_listar_creadores_seguidos_novedades(BIGINT)
    IS 'Devuelve los creadores seguidos por el usuario con su resumen de novedades.';
