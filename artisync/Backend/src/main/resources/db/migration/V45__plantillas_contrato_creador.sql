-- ==============================================================================
-- MIGRACIÓN V45: EL CREADOR PUEDE CREAR SUS PROPIAS PLANTILLAS DE ACUERDO
-- ==============================================================================
--
-- Hasta V39 el catálogo de plantillas_contrato era 100% curado por ADMIN: el
-- creador solo elegía entre esas plantillas al crear/editar su servicio
-- (ver comentario en ServicioCatalogoServicioImpl.resolverPlantillaContratoActiva).
--
-- Esta migración NO cambia esa curación admin — sigue existiendo y sigue
-- siendo obligatorio que haya una predeterminada (ux_plantilla_contrato_predeterminada
-- de V39 no se toca). Lo que agrega es una segunda vía, opcional: el creador
-- también puede dar de alta plantillas propias, visibles y usables solo por
-- él mismo (nunca por otro creador, nunca como predeterminada del catálogo
-- general). id_creador NULL sigue significando "plantilla del catálogo
-- curado por ADMIN"; no NULL significa "plantilla privada de ese creador".

ALTER TABLE plantillas_contrato
    ADD COLUMN IF NOT EXISTS id_creador BIGINT REFERENCES usuarios(id_usuario);

CREATE INDEX IF NOT EXISTS idx_plantillas_contrato_id_creador ON plantillas_contrato (id_creador);

-- ------------------------------------------------------------------------------
-- Permiso nuevo: gestionar las plantillas de acuerdo propias (solo CREADOR)
-- ------------------------------------------------------------------------------
-- Autoservicio, mismo criterio que FLUJO_GESTIONAR (V10/V28): el creador
-- gestiona SUS plantillas, nunca las de otro ni el catálogo general de ADMIN
-- (ese sigue exigiendo CONTRATO_PLANTILLA_GESTIONAR).
INSERT INTO permisos (nombre_permiso, modulo_aplicacion)
VALUES
    ('CONTRATO_PLANTILLA_PROPIA_GESTIONAR', 'SISTEMA')
ON CONFLICT (nombre_permiso) DO UPDATE
SET modulo_aplicacion = EXCLUDED.modulo_aplicacion;

INSERT INTO rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM roles r, permisos p
WHERE r.nombre_rol = 'CREADOR'
  AND p.nombre_permiso = 'CONTRATO_PLANTILLA_PROPIA_GESTIONAR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
