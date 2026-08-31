-- =============================================================================
-- V21: Generaliza la verificación de identidad de "solo Creador" a "cualquier
-- Usuario" (REQ-F-006 ampliado): un Cliente también necesita poder verificar
-- su identidad, y no tiene perfil de creador para colgar el certificado.
--
-- certificados_ia pasa de colgar de perfiles_creadores(id_perfil) a colgar
-- directamente de usuarios(id_usuario). Para un Creador, su perfil se sigue
-- pudiendo derivar con un JOIN contra perfiles_creadores.id_usuario cuando
-- haga falta (p. ej. certificados de tipo CERTIFICADO, que siguen siendo
-- inherentemente de creador).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Nueva columna id_usuario, retro-poblada desde el perfil actual.
-- -----------------------------------------------------------------------------
ALTER TABLE certificados_ia
    ADD COLUMN IF NOT EXISTS id_usuario BIGINT REFERENCES usuarios(id_usuario) ON DELETE CASCADE;

UPDATE certificados_ia c
SET id_usuario = pc.id_usuario
FROM perfiles_creadores pc
WHERE pc.id_perfil = c.id_perfil
  AND c.id_usuario IS NULL;

-- Salvaguarda: si algún certificado quedara sin id_usuario (perfil borrado sin
-- CASCADE en algún entorno viejo), la migración debe fallar de forma ruidosa
-- en vez de dejar una fila huérfana silenciosa.
DO $$
DECLARE
    huerfanos INT;
BEGIN
    SELECT COUNT(*) INTO huerfanos FROM certificados_ia WHERE id_usuario IS NULL;
    IF huerfanos > 0 THEN
        RAISE EXCEPTION 'V21: % certificados_ia sin id_perfil resoluble a un usuario; revisar manualmente antes de continuar.', huerfanos;
    END IF;
END
$$;

ALTER TABLE certificados_ia
    ALTER COLUMN id_usuario SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_certificados_ia_usuario ON certificados_ia(id_usuario);

-- -----------------------------------------------------------------------------
-- 2. Retirar id_perfil: ya no es la referencia de propiedad del certificado.
-- -----------------------------------------------------------------------------
ALTER TABLE certificados_ia
    DROP CONSTRAINT IF EXISTS certificados_ia_id_perfil_fkey;

ALTER TABLE certificados_ia
    DROP COLUMN IF EXISTS id_perfil;

-- -----------------------------------------------------------------------------
-- 3. fn_listar_cola_verificacion: unir contra usuarios directo, sin pasar por
--    perfiles_creadores (que ya no aplica para certificados de un Cliente).
--    DROP explícito porque cambia la forma de las columnas de retorno.
-- -----------------------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_listar_cola_verificacion(VARCHAR, INT, INT);

CREATE FUNCTION fn_listar_cola_verificacion(
    p_estado  VARCHAR,
    p_limite  INT,
    p_offset  INT
)
RETURNS TABLE (
    id_certificado        BIGINT,
    id_usuario            BIGINT,
    nombre_usuario        VARCHAR,
    tipo_documento        VARCHAR,
    nombre_estado         VARCHAR,
    veredicto_ia          VARCHAR,
    puntaje_confianza_ia  DECIMAL,
    fecha_analisis        TIMESTAMP
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id_certificado,
        c.id_usuario,
        (u.nombres || ' ' || u.apellidos)::VARCHAR,
        c.tipo_documento,
        ev.nombre_estado,
        c.veredicto_ia,
        c.puntaje_confianza_ia,
        c.fecha_analisis
    FROM certificados_ia c
    JOIN usuarios u ON u.id_usuario = c.id_usuario
    JOIN estados_verificacion ev ON ev.id_estado_verificacion = c.id_estado_verificacion
    WHERE p_estado IS NULL OR ev.nombre_estado = p_estado
    ORDER BY c.fecha_analisis ASC
    LIMIT p_limite OFFSET p_offset;
END;
$$;

-- sp_registrar_decision_verificacion no referencia perfil; no necesita cambios.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_app') THEN
        GRANT EXECUTE ON FUNCTION fn_listar_cola_verificacion(VARCHAR, INT, INT) TO artisync_app;
    END IF;
END
$$;
