-- =============================================================================
-- sp_purgar_notificaciones
-- Categoria funcional: actualizaciones masivas (mantenimiento)   Requisito: H-08 (auditoria de estado 2026-08-26)
-- =============================================================================
-- Purga por lotes las notificaciones_sistema ya leidas y con mas de
-- P_DIAS_RETENCION dias de antiguedad. Es la unica tabla de alto volumen de
-- escritura del sistema para la que existe una via de purga segura hoy:
--
--   - auditoria_eventos NO se purga por diseno (REQ-NF-013): esta protegida
--     por dos triggers de inmutabilidad (BEFORE UPDATE OR DELETE y BEFORE
--     TRUNCATE, fn_bloquear_modificacion_auditoria en V15) y por un REVOKE
--     explicito del privilegio DELETE a artisync_app. Purgarla exigiria
--     particionar la tabla y hacer DETACH+DROP PARTITION, un cambio
--     estructural fuera de alcance de este hallazgo.
--   - mensajes queda fuera: documentos_adjuntos referencia a mensajes con
--     ON DELETE CASCADE, y esos adjuntos tienen archivos en storage externo
--     (Azure Blob / disco local) que no se limpian solos -- un DELETE aqui
--     dejaria blobs huerfanos sin nadie que los borre.
--
-- notificaciones_sistema no tiene ninguna FK entrante (nada la referencia) y
-- no tiene trigger alguno, asi que es segura de purgar sin efectos
-- colaterales en otras tablas.
--
-- Es un PROCEDURE, no una FUNCTION, por el mismo motivo que
-- sp_purgar_datos_seguridad: necesita COMMIT real por lote para no mantener
-- una transaccion de larga duracion que bloquee a VACUUM mientras dura.
-- FOR UPDATE SKIP LOCKED + ORDER BY <pk> ASC + LIMIT es el mismo patron de
-- lotes ya usado alli.
--
-- A PROPOSITO no hay un bloque EXCEPTION en este procedimiento (mismo motivo
-- documentado en sp_purgar_datos_seguridad): un BEGIN...EXCEPTION...END en
-- PL/pgSQL abre un SAVEPOINT implicito, y PostgreSQL prohibe COMMIT mientras
-- ese SAVEPOINT sigue abierto.
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE PROCEDURE sp_purgar_notificaciones(
    p_tamano_lote INTEGER DEFAULT 1000,
    p_dias_retencion INTEGER DEFAULT 90
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_borradas INTEGER;
BEGIN
    IF p_tamano_lote IS NULL OR p_tamano_lote <= 0 THEN
        RAISE EXCEPTION 'sp_purgar_notificaciones: p_tamano_lote debe ser un entero positivo'
            USING ERRCODE = '22004';
    END IF;

    IF p_dias_retencion IS NULL OR p_dias_retencion <= 0 THEN
        RAISE EXCEPTION 'sp_purgar_notificaciones: p_dias_retencion debe ser un entero positivo'
            USING ERRCODE = '22004';
    END IF;

    -- Notificaciones ya leidas y con mas de p_dias_retencion dias de antiguedad.
    -- Las no leidas nunca se tocan, sin importar su edad: el usuario todavia
    -- no las vio.
    LOOP
        DELETE FROM notificaciones_sistema
         WHERE id_notificacion IN (
               SELECT id_notificacion
                 FROM notificaciones_sistema
                WHERE esta_leida = TRUE
                  AND fecha_emision < CURRENT_TIMESTAMP - (p_dias_retencion || ' days')::INTERVAL
                ORDER BY id_notificacion
                LIMIT p_tamano_lote
                FOR UPDATE SKIP LOCKED);

        GET DIAGNOSTICS v_borradas = ROW_COUNT;
        COMMIT;
        EXIT WHEN v_borradas = 0;
    END LOOP;
END;
$$;

COMMENT ON PROCEDURE sp_purgar_notificaciones(INTEGER, INTEGER)
    IS 'H-08: purga por lotes (COMMIT real por lote, FOR UPDATE SKIP LOCKED) las notificaciones_sistema leidas con mas de p_dias_retencion dias. auditoria_eventos y mensajes quedan fuera de alcance (ver cabecera del archivo).';

-- -----------------------------------------------------------------------------
-- Privilegios: igual que sp_purgar_datos_seguridad. ALTER DEFAULT PRIVILEGES
-- ... GRANT EXECUTE ON FUNCTIONS de seed_privilegios.sh cubre las FUNCTION de
-- este directorio, pero un PROCEDURE requiere su propio GRANT EXECUTE ON
-- PROCEDURE explicito. Guardado tras la existencia del rol.
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_app') THEN
        GRANT EXECUTE ON PROCEDURE sp_purgar_notificaciones(INTEGER, INTEGER) TO artisync_app;
    END IF;
END
$$;
