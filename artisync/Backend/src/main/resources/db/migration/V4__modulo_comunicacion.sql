-- =============================================================================
-- V4: Módulo 6 — Comunicación y Notificaciones
-- RF-15: Ampliar infracciones_mensaje con mensaje original y patrón detectado
-- RF-16: Tablas de briefing interactivo
-- =============================================================================

-- -----------------------------------------------------------------------------
-- RF-15: Columnas adicionales en infracciones_mensaje
-- -----------------------------------------------------------------------------
ALTER TABLE infracciones_mensaje
    ADD COLUMN IF NOT EXISTS mensaje_original   TEXT,
    ADD COLUMN IF NOT EXISTS patron_detectado   VARCHAR(50);

-- -----------------------------------------------------------------------------
-- RF-16: Briefing — Plantillas configuradas por el Creador
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS briefing_plantillas (
    id_briefing_plantilla BIGSERIAL PRIMARY KEY,
    id_perfil             BIGINT      NOT NULL REFERENCES perfiles_creadores(id_perfil) ON DELETE CASCADE,
    nombre_plantilla      VARCHAR(150) NOT NULL,
    fecha_creacion        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- RF-16: Briefing — Preguntas de una plantilla (máximo 10 por plantilla)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS briefing_preguntas (
    id_pregunta           BIGSERIAL PRIMARY KEY,
    id_briefing_plantilla BIGINT  NOT NULL REFERENCES briefing_plantillas(id_briefing_plantilla) ON DELETE CASCADE,
    texto_pregunta        TEXT NOT NULL,
    numero_orden          INT  NOT NULL,
    CONSTRAINT chk_orden_positivo CHECK (numero_orden > 0)
);

-- -----------------------------------------------------------------------------
-- RF-16: Briefing — Formulario enviado a un pedido
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS briefing_enviados (
    id_briefing_enviado   BIGSERIAL PRIMARY KEY,
    id_pedido             BIGINT    NOT NULL REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    id_briefing_plantilla BIGINT    NOT NULL REFERENCES briefing_plantillas(id_briefing_plantilla),
    fecha_envio           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completado            BOOLEAN   NOT NULL DEFAULT FALSE
);

-- -----------------------------------------------------------------------------
-- RF-16: Briefing — Respuestas del Cliente (inmutables tras envío)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS briefing_respuestas (
    id_respuesta         BIGSERIAL PRIMARY KEY,
    id_briefing_enviado  BIGINT    NOT NULL REFERENCES briefing_enviados(id_briefing_enviado) ON DELETE CASCADE,
    id_pregunta          BIGINT    NOT NULL REFERENCES briefing_preguntas(id_pregunta),
    texto_respuesta      TEXT      NOT NULL,
    fecha_respuesta      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_briefing_enviado, id_pregunta)
);
