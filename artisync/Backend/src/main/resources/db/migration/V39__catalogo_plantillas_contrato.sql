-- ==============================================================================
-- MIGRACIÓN V39: CATÁLOGO DE PLANTILLAS DE CONTRATO POR SERVICIO
-- ==============================================================================
--
-- Hasta ahora existía una única plantilla de contrato (sembrada en V13), y
-- ContratoServicioImpl.generarContrato() siempre tomaba "la de mayor id": todo
-- servicio firmaba exactamente el mismo texto legal, sin importar su rubro.
--
-- Esta migración introduce un catálogo de plantillas curado por ADMIN (no un
-- editor libre por creador, para no exponer a la plataforma a cláusulas
-- legales no revisadas). El creador elige, al crear/editar su servicio, cuál
-- plantilla del catálogo aplica; si no elige ninguna, se usa la marcada como
-- predeterminada. Mismo patrón que V35 (FK nullable en servicios, sin
-- bloquear nada si no está configurada).

ALTER TABLE plantillas_contrato
    ADD COLUMN IF NOT EXISTS nombre_plantilla  VARCHAR(150) NOT NULL DEFAULT 'General',
    ADD COLUMN IF NOT EXISTS es_predeterminada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS activa             BOOLEAN NOT NULL DEFAULT TRUE;

-- La plantilla sembrada en V13 pasa a ser la predeterminada de respaldo.
UPDATE plantillas_contrato
SET es_predeterminada = TRUE,
    nombre_plantilla  = 'General (predeterminada)'
WHERE version_legal = 'v1.0';

-- Solo una plantilla puede ser la predeterminada a la vez.
CREATE UNIQUE INDEX IF NOT EXISTS ux_plantilla_contrato_predeterminada
    ON plantillas_contrato (es_predeterminada)
    WHERE es_predeterminada = TRUE;

-- ------------------------------------------------------------------------------
-- Relación servicio → plantilla de contrato
-- ------------------------------------------------------------------------------
-- Nullable a propósito: un servicio sin plantilla asignada cae a la
-- predeterminada en ContratoServicioImpl, sin bloquear la generación del
-- contrato.
ALTER TABLE servicios
    ADD COLUMN IF NOT EXISTS id_plantilla_contrato BIGINT REFERENCES plantillas_contrato(id_plantilla);

CREATE INDEX IF NOT EXISTS idx_servicios_id_plantilla_contrato ON servicios (id_plantilla_contrato);

-- ------------------------------------------------------------------------------
-- Permiso nuevo: gestionar el catálogo de plantillas de contrato (solo ADMIN)
-- ------------------------------------------------------------------------------
-- Idempotente por el mismo motivo que V10/V29: convive con db/seed.sql
-- aplicado en el arranque de Docker.
INSERT INTO permisos (nombre_permiso, modulo_aplicacion)
VALUES
    ('CONTRATO_PLANTILLA_GESTIONAR', 'SISTEMA')
ON CONFLICT (nombre_permiso) DO UPDATE
SET modulo_aplicacion = EXCLUDED.modulo_aplicacion;

INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'ADMIN'
  AND p.nombre_permiso = 'CONTRATO_PLANTILLA_GESTIONAR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
