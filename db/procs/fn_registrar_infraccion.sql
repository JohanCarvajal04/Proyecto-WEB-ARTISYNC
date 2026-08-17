-- =============================================================================
-- fn_registrar_infraccion
-- Categoria funcional: calculos agregados + validacion cruzada     Requisito: REQ-F-015
-- =============================================================================
-- Registra una infraccion de mensaje (RF-15: intento de compartir datos de
-- contacto fuera del chat de la plataforma) y, en la misma transaccion,
-- cuenta cuantas infracciones acumula el usuario en la ventana movil de los
-- ultimos 30 dias; si alcanza 3 o mas, suspende la cuenta automaticamente.
-- Sustituye a InfraccionServiceImpl.registrarInfraccion/suspenderCuenta, que
-- hacia un INSERT, un COUNT y un UPDATE condicional como tres llamadas
-- independientes al repositorio.
--
-- Por que en el motor: el conteo y la suspension deben ser consistentes con
-- el INSERT que las precede -- si dos infracciones del mismo usuario llegan
-- casi al mismo tiempo (dos mensajes filtrados en paralelo), cada llamada
-- debe ver el efecto de la anterior. La fila de usuarios se toma con
-- FOR UPDATE antes de decidir la suspension para serializar esa carrera.
--
-- El filtrado de patrones (deteccion de telefonos/correos/redes en el texto
-- del mensaje) permanece en Java (MensajeFilterService): es logica de texto,
-- no de datos, y no pertenece al motor.
--
-- Devuelve JSONB: { idInfraccion, totalInfraccionesPeriodo, cuentaSuspendida }.
-- cuentaSuspendida es TRUE solo en la llamada que cruza el umbral (no se
-- repite el efecto de notificacion en llamadas posteriores porque la cuenta
-- ya estara con estado_cuenta = FALSE y no se vuelve a marcar suspendida).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_registrar_infraccion(
    p_id_usuario        BIGINT,
    p_id_pedido         BIGINT,
    p_mensaje_original  TEXT,
    p_patron_detectado  VARCHAR(50)
)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_infraccion       BIGINT;
    v_total_periodo       INTEGER;
    v_estado_cuenta       BOOLEAN;
    v_cuenta_suspendida   BOOLEAN := FALSE;
    v_max_infracciones    CONSTANT INTEGER := 3;
    v_periodo_dias        CONSTANT INTEGER := 30;
BEGIN
    IF p_id_usuario IS NULL THEN
        RAISE EXCEPTION 'fn_registrar_infraccion: p_id_usuario es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = p_id_usuario) THEN
        RAISE EXCEPTION 'Usuario no encontrado: %', p_id_usuario
            USING ERRCODE = 'P0002';
    END IF;

    INSERT INTO infracciones_mensaje (id_usuario, id_pedido, mensaje_original, patron_detectado, fecha_infraccion)
    VALUES (p_id_usuario, p_id_pedido, p_mensaje_original, p_patron_detectado, CURRENT_TIMESTAMP)
    RETURNING id_infraccion INTO v_id_infraccion;

    SELECT COUNT(*) INTO v_total_periodo
      FROM infracciones_mensaje
     WHERE id_usuario = p_id_usuario
       AND fecha_infraccion > CURRENT_TIMESTAMP - (v_periodo_dias || ' days')::interval;

    IF v_total_periodo >= v_max_infracciones THEN
        SELECT estado_cuenta INTO v_estado_cuenta
          FROM usuarios
         WHERE id_usuario = p_id_usuario
           FOR UPDATE;

        IF v_estado_cuenta IS TRUE THEN
            UPDATE usuarios SET estado_cuenta = FALSE WHERE id_usuario = p_id_usuario;
            v_cuenta_suspendida := TRUE;
        END IF;
    END IF;

    RETURN jsonb_build_object(
        'idInfraccion', v_id_infraccion,
        'totalInfraccionesPeriodo', v_total_periodo,
        'cuentaSuspendida', v_cuenta_suspendida
    );
END;
$$;

COMMENT ON FUNCTION fn_registrar_infraccion(BIGINT, BIGINT, TEXT, VARCHAR)
    IS 'REQ-F-015 - Calculo agregado + validacion cruzada: registra una infraccion de mensaje, cuenta las del usuario en 30 dias y suspende la cuenta automaticamente al llegar a 3.';
