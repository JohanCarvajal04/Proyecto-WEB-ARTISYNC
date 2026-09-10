-- ==============================================================================
-- ARTISYNC — Diferenciar respaldos FULL vs DIARIO (categoría ortogonal a tipo)
-- ==============================================================================

-- 1. Categoría del respaldo (FULL/DIARIO), ortogonal a tipo (MANUAL/AUTOMATICO)
ALTER TABLE respaldos_bd ADD COLUMN categoria VARCHAR(20);

-- Backfill: MANUAL -> FULL, AUTOMATICO -> DIARIO (mapeo histórico razonable,
-- ya que hoy la única programación automática existente es diaria)
UPDATE respaldos_bd SET categoria = CASE WHEN tipo = 'MANUAL' THEN 'FULL' ELSE 'DIARIO' END;

ALTER TABLE respaldos_bd ALTER COLUMN categoria SET NOT NULL;
ALTER TABLE respaldos_bd ADD CONSTRAINT respaldos_bd_categoria_check
    CHECK (categoria IN ('FULL', 'DIARIO'));

CREATE INDEX idx_respaldos_categoria ON respaldos_bd(categoria);

-- 2. Política: dos programaciones automáticas independientes (full y diaria)
ALTER TABLE respaldos_politica
    ADD COLUMN cron_full VARCHAR(100) NOT NULL DEFAULT '0 0 3 * * SUN',
    ADD COLUMN retencion_dias_full INTEGER NOT NULL DEFAULT 90,
    ADD COLUMN cron_diario VARCHAR(100),
    ADD COLUMN retencion_dias_diario INTEGER;

-- Migrar la programación única existente al slot "diario" (comportamiento histórico)
UPDATE respaldos_politica
    SET cron_diario = cron_expresion, retencion_dias_diario = retencion_dias
    WHERE id_politica = 1;

ALTER TABLE respaldos_politica
    ALTER COLUMN cron_diario SET NOT NULL,
    ALTER COLUMN retencion_dias_diario SET NOT NULL;

ALTER TABLE respaldos_politica DROP COLUMN cron_expresion;
ALTER TABLE respaldos_politica DROP COLUMN retencion_dias;
