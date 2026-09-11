-- =============================================================================
-- V48: integridad de contenido y retención de contratos firmados (REQ-NF-020).
-- =============================================================================
-- generarHashFirma() hashea idContrato:idUsuario:Instant.now() -- una huella
-- del EVENTO de firma (quien/cuando), no del CONTENIDO del contrato. Al incluir
-- la hora exacta, recalcularlo nunca reproduce el mismo valor, así que no hay
-- nada que "re-verificar" contra ese hash tal como esta definido. Se introduce
-- un hash de CONTENIDO nuevo y separado (hash_contenido), calculado sobre un
-- snapshot congelado del HTML ya renderizado (contenido_congelado) en el
-- momento en que el contrato queda firmado por ambas partes -- nunca se
-- vuelve a renderizar desde la plantilla, porque generarContratoHtml()
-- incluye {{fecha_actual}} = LocalDate.now(), que cambiaria cada dia.
ALTER TABLE contratos
    ADD COLUMN IF NOT EXISTS contenido_congelado    TEXT,
    ADD COLUMN IF NOT EXISTS hash_contenido          VARCHAR(64),
    ADD COLUMN IF NOT EXISTS fecha_hash_contenido    TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_limite_retencion  TIMESTAMP;
