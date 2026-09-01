-- =============================================================================
-- V23: CHECK en certificados_ia.puntaje_confianza_ia (rango [0.00, 1.00]).
--
-- Antes el rango solo se validaba en Java (@DecimalMin/@DecimalMax en
-- CertificadoIa.java): un UPDATE directo, un procedimiento almacenado, o
-- cualquier acceso que no pase por esa entidad podía dejar un puntaje fuera
-- de rango sin que nadie lo impidiera. La columna sigue siendo nullable (un
-- certificado puede no tener puntaje aún), por eso el CHECK solo restringe
-- el valor cuando no es NULL.
-- =============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_certificados_ia_puntaje_confianza_rango'
    ) THEN
        ALTER TABLE certificados_ia
            ADD CONSTRAINT ck_certificados_ia_puntaje_confianza_rango
            CHECK (puntaje_confianza_ia IS NULL
                OR (puntaje_confianza_ia >= 0.00 AND puntaje_confianza_ia <= 1.00));
    END IF;
END $$;
