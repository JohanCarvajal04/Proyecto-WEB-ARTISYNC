-- =============================================================================
-- V6: Verificación asistida por IA (REQ-F-006, REQ-F-007)
-- La IA asiste al moderador; nunca decide. El estado final lo escribe siempre
-- una persona con permiso CERTIFICADO_REVISAR — ver
-- docs/superpowers/specs/2026-08-06-ia-verificacion-asistida-design.md
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Seed de estados_verificacion (tabla creada en V1, nunca sembrada)
-- -----------------------------------------------------------------------------
INSERT INTO estados_verificacion (nombre_estado)
VALUES ('PENDIENTE'), ('APROBADO'), ('RECHAZADO'), ('REQUIERE_ACLARACION')
ON CONFLICT (nombre_estado) DO NOTHING;

-- -----------------------------------------------------------------------------
-- certificados_ia: separar el dictamen de la IA de la decisión humana
-- -----------------------------------------------------------------------------
ALTER TABLE certificados_ia
    ADD COLUMN IF NOT EXISTS tipo_documento       VARCHAR(20) NOT NULL DEFAULT 'IDENTIDAD',
    ADD COLUMN IF NOT EXISTS hash_documento        VARCHAR(64),
    ADD COLUMN IF NOT EXISTS veredicto_ia          VARCHAR(30),
    ADD COLUMN IF NOT EXISTS razon_ia               TEXT,
    ADD COLUMN IF NOT EXISTS datos_extraidos_ia     TEXT,
    ADD COLUMN IF NOT EXISTS fecha_dictamen_ia       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS id_moderador           BIGINT REFERENCES usuarios(id_usuario),
    ADD COLUMN IF NOT EXISTS fecha_decision          TIMESTAMP,
    ADD COLUMN IF NOT EXISTS nota_moderador           TEXT,
    ADD COLUMN IF NOT EXISTS documento_eliminado      BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE certificados_ia
    DROP CONSTRAINT IF EXISTS chk_certificados_ia_tipo_documento;
ALTER TABLE certificados_ia
    ADD CONSTRAINT chk_certificados_ia_tipo_documento
        CHECK (tipo_documento IN ('IDENTIDAD', 'CERTIFICADO'));

CREATE INDEX IF NOT EXISTS idx_certificados_ia_hash ON certificados_ia(hash_documento);
CREATE INDEX IF NOT EXISTS idx_certificados_ia_estado ON certificados_ia(id_estado_verificacion);

-- -----------------------------------------------------------------------------
-- Función SQL: cola de revisión (join certificados_ia + perfiles_creadores +
-- usuarios + estados_verificacion). tipo_acceso=SP en la matriz de
-- trazabilidad para REQ-F-006/007 (ADR-006).
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_listar_cola_verificacion(
    p_estado  VARCHAR,
    p_limite  INT,
    p_offset  INT
)
RETURNS TABLE (
    id_certificado        BIGINT,
    id_perfil             BIGINT,
    nombre_creador        VARCHAR,
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
        c.id_perfil,
        (u.nombres || ' ' || u.apellidos)::VARCHAR,
        c.tipo_documento,
        ev.nombre_estado,
        c.veredicto_ia,
        c.puntaje_confianza_ia,
        c.fecha_analisis
    FROM certificados_ia c
    JOIN perfiles_creadores pc ON pc.id_perfil = c.id_perfil
    JOIN usuarios u ON u.id_usuario = pc.id_usuario
    JOIN estados_verificacion ev ON ev.id_estado_verificacion = c.id_estado_verificacion
    WHERE p_estado IS NULL OR ev.nombre_estado = p_estado
    ORDER BY c.fecha_analisis ASC
    LIMIT p_limite OFFSET p_offset;
END;
$$;

GRANT EXECUTE ON FUNCTION fn_listar_cola_verificacion(VARCHAR, INT, INT) TO artisync_app;

-- -----------------------------------------------------------------------------
-- Procedimiento: registrar la decisión del moderador. Único punto de
-- escritura de id_estado_verificacion; valida existencia de certificado,
-- estado y moderador antes de escribir (validación cruzada, ADR-006).
-- El archivo físico lo borra la capa Java tras invocar este procedimiento.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE sp_registrar_decision_verificacion(
    p_id_certificado  BIGINT,
    p_id_estado       BIGINT,
    p_id_moderador    BIGINT,
    p_nota            TEXT
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM certificados_ia WHERE id_certificado = p_id_certificado) THEN
        RAISE EXCEPTION 'Certificado de verificación % no existe', p_id_certificado;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM estados_verificacion WHERE id_estado_verificacion = p_id_estado) THEN
        RAISE EXCEPTION 'Estado de verificación % no existe', p_id_estado;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = p_id_moderador) THEN
        RAISE EXCEPTION 'Moderador % no existe', p_id_moderador;
    END IF;

    UPDATE certificados_ia
    SET id_estado_verificacion = p_id_estado,
        id_moderador           = p_id_moderador,
        fecha_decision          = CURRENT_TIMESTAMP,
        nota_moderador           = p_nota,
        documento_eliminado      = TRUE
    WHERE id_certificado = p_id_certificado;
END;
$$;

GRANT EXECUTE ON PROCEDURE sp_registrar_decision_verificacion(BIGINT, BIGINT, BIGINT, TEXT) TO artisync_app;
