-- =============================================================================
-- V31: corrige TOCTOU en sp_registrar_decision_verificacion (revisión técnica
-- 2026-09-01)
--
-- El SELECT que comprobaba el estado actual del certificado no bloqueaba la
-- fila (sin FOR UPDATE), y el UPDATE posterior solo filtraba por
-- id_certificado, sin repetir la condición de estado. Si dos moderadores
-- registraban una decisión sobre el mismo certificado casi simultáneamente,
-- ambos SELECT podían leer PENDIENTE antes de que cualquiera confirmara: el
-- segundo UPDATE se bloqueaba por el lock de fila del primero, pero al
-- ejecutarse sobrescribía silenciosamente la decisión ya tomada (estado,
-- moderador, nota, documento_eliminado) en vez de lanzar la excepción
-- esperada de "no está en PENDIENTE".
--
-- El arreglo agrega FOR UPDATE al SELECT inicial, mismo patrón que ya usan
-- fn_seleccionar_ganadores_sorteo y fn_registrar_infraccion: la segunda
-- llamada concurrente espera a que la primera confirme, y al re-leer ve el
-- estado ya cambiado, lanzando correctamente la excepción.
-- =============================================================================

CREATE OR REPLACE PROCEDURE sp_registrar_decision_verificacion(
    p_id_certificado  BIGINT,
    p_id_estado       BIGINT,
    p_id_moderador    BIGINT,
    p_nota            TEXT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nombre_estado_actual VARCHAR;
    v_nombre_estado_nuevo  VARCHAR;
BEGIN
    SELECT ev.nombre_estado INTO v_nombre_estado_actual
    FROM certificados_ia c
    JOIN estados_verificacion ev ON ev.id_estado_verificacion = c.id_estado_verificacion
    WHERE c.id_certificado = p_id_certificado
    FOR UPDATE OF c;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Certificado de verificación % no existe', p_id_certificado;
    END IF;

    IF v_nombre_estado_actual <> 'PENDIENTE' THEN
        RAISE EXCEPTION 'El certificado % no está en estado PENDIENTE (estado actual: %); no se puede registrar una decisión sobre él nuevamente.',
            p_id_certificado, v_nombre_estado_actual;
    END IF;

    SELECT nombre_estado INTO v_nombre_estado_nuevo
    FROM estados_verificacion
    WHERE id_estado_verificacion = p_id_estado;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Estado de verificación % no existe', p_id_estado;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = p_id_moderador) THEN
        RAISE EXCEPTION 'Moderador % no existe', p_id_moderador;
    END IF;

    UPDATE certificados_ia
    SET id_estado_verificacion = p_id_estado,
        id_moderador           = p_id_moderador,
        fecha_decision          = CURRENT_TIMESTAMP,
        nota_moderador           = p_nota,
        documento_eliminado      = (v_nombre_estado_nuevo IN ('APROBADO', 'RECHAZADO'))
    WHERE id_certificado = p_id_certificado;
END;
$$;
