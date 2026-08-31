-- =============================================================================
-- V30: Corregir el XHTML mal formado de la plantilla de contrato (v1.0)
-- =============================================================================
--
-- PdfGeneracionServicioImpl usa openhtmltopdf, que exige XHTML estrictamente
-- bien formado (parser XML, no HTML5 tolerante). La plantilla sembrada por
-- V13__seed_plantilla_contrato.sql tiene `<meta charset="UTF-8">` sin
-- autocerrar; el parser lanza SAXParseException al renderizar CUALQUIER
-- contrato, que PdfGeneracionServicioImpl atrapa y reenvía como un 500
-- genérico ("Error interno del servidor"), sin mensaje útil para el usuario.
--
-- No se puede corregir V13 directamente (ya se aplicó en entornos existentes
-- y Flyway rechaza modificar una migración ya ejecutada por checksum), así
-- que esta migración actualiza en sitio el HTML ya sembrado para la versión
-- legal 'v1.0'. Es idempotente: si el HTML ya está corregido, el UPDATE no
-- cambia nada.

UPDATE plantillas_contrato
SET cuerpo_html_plantilla = REPLACE(
    cuerpo_html_plantilla,
    '<meta charset="UTF-8"><title>',
    '<meta charset="UTF-8"/><title>'
)
WHERE version_legal = 'v1.0';
