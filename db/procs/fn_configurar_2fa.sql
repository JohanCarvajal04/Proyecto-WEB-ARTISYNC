-- =============================================================================
-- fn_configurar_2fa
-- Categoria funcional: validaciones cruzadas + escritura multi-tabla   Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §7 — corrige la anomalia A4.
-- =============================================================================
-- Inicia o reinicia la configuracion de 2FA de un usuario: fija la nueva
-- llave secreta TOTP y reemplaza el juego completo de codigos de respaldo.
--
-- Sustituye a TwoFactorServiceImpl.setup2Fa (parte de escritura): upsert de
-- autenticacion_dos_factores + DELETE de codigos anteriores + 8 INSERT
-- individuales de codigos_respaldo_2fa -- 10 viajes no atomicos a la base. Si
-- el proceso fallaba a mitad del bucle de codigos (timeout, caida de
-- conexion), el usuario quedaba con un secreto TOTP nuevo pero un juego de
-- codigos de respaldo incompleto, sin ningun aviso.
--
-- El upsert usa ON CONFLICT sobre autenticacion_dos_factores.id_usuario, que
-- ya es UNIQUE (V1__schema_inicial.sql): no hay ventana entre "verificar si
-- existe" e "insertar o actualizar". El borrado + alta de los 8 codigos ocurre
-- en la MISMA transaccion que el upsert del secreto: es imposible observar un
-- secreto nuevo emparejado con codigos de respaldo del secreto anterior.
--
-- Los codigos de respaldo llegan ya hasheados (SHA-256, calculado en Java
-- antes de invocar esta funcion, igual que en fn_restablecer_contrasena): la
-- funcion nunca ve el codigo en texto plano.
--
-- Devuelve el numero de codigos de respaldo insertados (normalmente 8).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_configurar_2fa(
    p_id_usuario    BIGINT,
    p_llave_secreta VARCHAR(255),
    p_hashes        TEXT[]
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_total INTEGER;
BEGIN
    IF p_id_usuario IS NULL OR p_llave_secreta IS NULL THEN
        RAISE EXCEPTION 'fn_configurar_2fa: p_id_usuario y p_llave_secreta son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = p_id_usuario) THEN
        RAISE EXCEPTION 'Usuario no encontrado con ID: %', p_id_usuario
            USING ERRCODE = 'P0002';
    END IF;

    -- Upsert atomico: nunca hay una ventana en la que el registro no exista y
    -- dos peticiones concurrentes de setup2Fa intenten ambas un INSERT.
    INSERT INTO autenticacion_dos_factores (id_usuario, llave_secreta, esta_habilitado)
    VALUES (p_id_usuario, p_llave_secreta, FALSE)
    ON CONFLICT (id_usuario)
    DO UPDATE SET llave_secreta = EXCLUDED.llave_secreta, esta_habilitado = FALSE;

    -- Borrado + alta de los codigos en la MISMA transaccion que el upsert de
    -- arriba: un secreto nuevo siempre viene acompanado de SU juego completo
    -- de codigos de respaldo, nunca de uno parcial o del anterior.
    DELETE FROM codigos_respaldo_2fa WHERE id_usuario = p_id_usuario;

    IF p_hashes IS NOT NULL AND array_length(p_hashes, 1) > 0 THEN
        INSERT INTO codigos_respaldo_2fa (id_usuario, codigo_hash, usado)
        SELECT p_id_usuario, h, FALSE
          FROM unnest(p_hashes) AS h;
    END IF;

    GET DIAGNOSTICS v_total = ROW_COUNT;
    RETURN v_total;
END;
$$;

COMMENT ON FUNCTION fn_configurar_2fa(BIGINT, VARCHAR, TEXT[])
    IS 'Fase 3 concurrencia - Upsert atomico del secreto TOTP + reemplazo completo de codigos de respaldo en una unica transaccion, eliminando el estado a medias (A4) de la version en 10 pasos.';
