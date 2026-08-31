-- =============================================================================
-- V26: la config de etapa puede exigir un entregable subido antes de avanzar
-- =============================================================================
-- Mismo patron que es_etapa_final (V1): un flag por combinacion flujo+etapa,
-- no en etapas_flujo, porque la misma etapa con el mismo nombre puede
-- reutilizarse en varios flujos con distinta exigencia.
--
-- PedidoServicioImpl#avanzarEtapa valida este flag contra
-- EntregableFinalRepository.existsByPedidoIdPedido antes de registrar la
-- transicion: sin entregable subido para el pedido, la etapa marcada no se
-- puede abandonar.
-- =============================================================================

ALTER TABLE flujo_etapas_config
    ADD COLUMN requiere_entregable BOOLEAN NOT NULL DEFAULT FALSE;

-- Datos demo (V25): "En Producción" pasa a exigir entregable en ambos flujos
-- semilla (Estándar y Express) antes de avanzar a revisión del cliente. Por
-- nombre de etapa y no por ID: V25 ya corrió en instalaciones existentes, así
-- que esta migración nueva es el único lugar seguro para tocar esos datos sin
-- romper el checksum de una migración ya aplicada.
UPDATE flujo_etapas_config fec
SET requiere_entregable = TRUE
FROM etapas_flujo ef
WHERE fec.id_etapa = ef.id_etapa
  AND ef.nombre_etapa = 'En Producción';
