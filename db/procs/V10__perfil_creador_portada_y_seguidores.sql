-- =============================================================================
-- V10__perfil_creador_portada_y_seguidores.sql
-- Estructuras de apoyo para portada del perfil de creador e indices de seguidores
-- =============================================================================

ALTER TABLE perfiles_creadores
    ADD COLUMN IF NOT EXISTS url_portada VARCHAR(500),
    ADD COLUMN IF NOT EXISTS titulo_profesional VARCHAR(150);

COMMENT ON COLUMN perfiles_creadores.url_portada
    IS 'URL de la imagen de portada/banner del perfil de creador';

COMMENT ON COLUMN perfiles_creadores.titulo_profesional
    IS 'Titulo o especialidad principal del creador (ej. Ilustradora & Directora de Arte)';

-- Indices de rendimiento para consultas de seguidores
CREATE INDEX IF NOT EXISTS idx_seguidores_perfil_creador
    ON seguidores (id_perfil_creador);

CREATE INDEX IF NOT EXISTS idx_seguidores_usuario_seguidor
    ON seguidores (id_usuario_seguidor);
