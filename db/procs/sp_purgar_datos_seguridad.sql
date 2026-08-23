-- =============================================================================
-- sp_purgar_datos_seguridad
-- Categoria funcional: actualizaciones masivas (mantenimiento)   Requisito: REQ-NF (concurrencia)
-- Fase 4 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §7 — corrige la anomalia A10.
-- =============================================================================
-- Purga por lotes los datos de seguridad que hoy crecen sin limite:
-- sesiones_usuario expiradas, tokens_recuperacion muertos y codigos_respaldo_2fa
-- ya consumidos. No existe hoy ninguna tarea de mantenimiento sobre estas tres
-- tablas -- el indice idx_sesiones_usuario_fecha_expiracion que V8 creo para
-- esto no lo usa nadie.
--
-- Es un PROCEDURE, no una FUNCTION, y es la UNICA rutina de db/procs/ que hace
-- COMMIT/ROLLBACK reales (ver docs/basedatos/PLAN-CONCURRENCIA-SP.md §0.3):
-- borrar en una sola transaccion un historial completo de sesiones/tokens
-- mantendria una transaccion de larga duracion que impide a VACUUM recuperar
-- espacio en TODA la base mientras dura. Se confirma un lote a la vez.
--
-- FOR UPDATE SKIP LOCKED es lo que hace la purga compatible con el trafico en
-- vivo: en vez de esperar a una fila que un login/logout concurrente tiene
-- bloqueada, la salta y la recoge en la siguiente ejecucion (se corre a diario,
-- no hay prisa). ORDER BY <pk> ASC antes del LIMIT mantiene el orden de borrado
-- estable entre lotes sucesivos.
--
-- Alcance deliberadamente ACOTADO en codigos_respaldo_2fa: solo se purgan los
-- YA CONSUMIDOS (usado = TRUE). Los no usados de un usuario con 2FA
-- deshabilitado NO se tocan aqui: la tabla no tiene una columna de fecha que
-- distinga un codigo "huerfano" (2FA desactivado hace tiempo) de uno recien
-- generado por fn_configurar_2fa a la espera de que el usuario llame a
-- confirm2Fa -- purgar por esta_habilitado = FALSE borraria codigos de una
-- configuracion de 2FA en curso, todavia sin confirmar, dejando al usuario sin
-- sus codigos de respaldo antes de haber podido guardarlos.
--
-- tokens_recuperacion: se purgan los ya usados y los generados hace mas de 24h
-- (la ventana de validez real es 60 minutos, ver fn_restablecer_contrasena
-- REQ-F-005); 24h da margen de sobra sin acumular tokens muertos indefinidamente.
--
-- No devuelve nada (PROCEDURE). Si una sentencia falla a mitad de un lote (p.
-- ej. deadlock, disco lleno), PostgreSQL aborta automaticamente la
-- transaccion en curso -- sin necesidad de un ROLLBACK explicito -- y los
-- lotes de las tablas anteriores que ya hicieron COMMIT permanecen intactos.
-- El error se propaga tal cual a traves del CALL hasta el llamante Java
-- (SeguridadPurgaScheduler), que ya lo captura y registra sin tumbar el
-- proceso; la siguiente ejecucion programada retoma desde donde quedo.
--
-- A PROPOSITO no hay un bloque EXCEPTION en este procedimiento: PL/pgSQL
-- implementa cada BEGIN...EXCEPTION...END como una subtransaccion respaldada
-- por un SAVEPOINT implicito, y PostgreSQL PROHIBE ejecutar COMMIT/ROLLBACK
-- mientras ese savepoint sigue abierto -- falla con
-- "invalid transaction termination" en el primer COMMIT del primer lote. Es
-- el mismo motivo por el que una FUNCTION no puede hacer COMMIT/ROLLBACK
-- (§0.1 de PLAN-CONCURRENCIA-SP.md), aplicado aqui a un bloque con manejo de
-- excepciones dentro de un PROCEDURE. El propio manual de PostgreSQL resuelve
-- esto separando ambos: la excepcion se atrapa en un bloque interno SIN
-- COMMIT, y el COMMIT ocurre en el bloque externo, fuera de cualquier
-- EXCEPTION -- exactamente la estructura de este archivo (tres LOOP con
-- COMMIT, ninguno envuelto en un manejador de excepciones).
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE PROCEDURE sp_purgar_datos_seguridad(
    p_tamano_lote INTEGER DEFAULT 1000
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_borradas INTEGER;
BEGIN
    IF p_tamano_lote IS NULL OR p_tamano_lote <= 0 THEN
        RAISE EXCEPTION 'sp_purgar_datos_seguridad: p_tamano_lote debe ser un entero positivo'
            USING ERRCODE = '22004';
    END IF;

    -- 1) Sesiones expiradas.
    LOOP
        DELETE FROM sesiones_usuario
         WHERE id_sesion IN (
               SELECT id_sesion
                 FROM sesiones_usuario
                WHERE fecha_expiracion < CURRENT_TIMESTAMP
                ORDER BY id_sesion
                LIMIT p_tamano_lote
                FOR UPDATE SKIP LOCKED);

        GET DIAGNOSTICS v_borradas = ROW_COUNT;
        COMMIT;
        EXIT WHEN v_borradas = 0;
    END LOOP;

    -- 2) Tokens de recuperacion muertos: usados, o con mas de 24h de antiguedad.
    LOOP
        DELETE FROM tokens_recuperacion
         WHERE id_token IN (
               SELECT id_token
                 FROM tokens_recuperacion
                WHERE usado = TRUE
                   OR fecha_generacion < CURRENT_TIMESTAMP - INTERVAL '24 hours'
                ORDER BY id_token
                LIMIT p_tamano_lote
                FOR UPDATE SKIP LOCKED);

        GET DIAGNOSTICS v_borradas = ROW_COUNT;
        COMMIT;
        EXIT WHEN v_borradas = 0;
    END LOOP;

    -- 3) Codigos de respaldo 2FA ya consumidos (ver nota de alcance arriba:
    --    los no usados no se tocan, para no interferir con un setup2Fa en curso).
    LOOP
        DELETE FROM codigos_respaldo_2fa
         WHERE id_codigo IN (
               SELECT id_codigo
                 FROM codigos_respaldo_2fa
                WHERE usado = TRUE
                ORDER BY id_codigo
                LIMIT p_tamano_lote
                FOR UPDATE SKIP LOCKED);

        GET DIAGNOSTICS v_borradas = ROW_COUNT;
        COMMIT;
        EXIT WHEN v_borradas = 0;
    END LOOP;
END;
$$;

COMMENT ON PROCEDURE sp_purgar_datos_seguridad(INTEGER)
    IS 'Fase 4 mantenimiento - Purga por lotes (COMMIT real por lote, FOR UPDATE SKIP LOCKED) sesiones expiradas, tokens de recuperacion muertos y codigos de respaldo 2FA consumidos. Corrige el crecimiento sin limite de A10.';

-- -----------------------------------------------------------------------------
-- Privilegios: igual que sp_registrar_decision_verificacion
-- (V7__verificacion_asistida_ia.sql), el unico otro PROCEDURE del proyecto.
-- ALTER DEFAULT PRIVILEGES ... GRANT EXECUTE ON FUNCTIONS de
-- seed_privilegios.sh cubre las FUNCTION de este directorio, pero un
-- PROCEDURE requiere su propio GRANT EXECUTE ON PROCEDURE explicito. Guardado
-- tras la existencia del rol: seed_privilegios.sh solo lo crea en el primer
-- arranque de un volumen de datos vacio; en una BD restaurada de un dump, en
-- CI o en una instancia gestionada ese rol no existe todavia.
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'artisync_app') THEN
        GRANT EXECUTE ON PROCEDURE sp_purgar_datos_seguridad(INTEGER) TO artisync_app;
    END IF;
END
$$;
