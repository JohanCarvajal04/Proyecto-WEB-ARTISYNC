-- =============================================================================
-- fn_revocar_sesiones_usuario
-- Categoria funcional: actualizaciones masivas                  Requisito: REQ-NF (concurrencia)
-- Fase 1 de docs/basedatos/PLAN-CONCURRENCIA-SP.md — corrige la anomalia A6.
-- =============================================================================
-- Borra todas las sesiones de un usuario y devuelve, en la misma sentencia,
-- el jti y el tiempo de vida restante de cada una que borro.
--
-- Sustituye la parte SQL de SessionRevocationService.revocarSesionesUsuario():
--   (1) List<SesionUsuario> sesiones = findByUsuarioIdUsuario(idUsuario);
--   (2) por cada sesion: revocarJtiEnRedis(...)
--   (3) deleteByUsuarioIdUsuario(idUsuario);
-- son tres sentencias separadas. Bajo READ COMMITTED cada una ve su propio
-- snapshot: una sesion CREADA entre (1) y (3) -- por ejemplo, un login
-- concurrente del mismo usuario justo cuando un administrador lo desactiva --
-- nunca aparece en la lista leida en (1), pero SI la borra el DELETE de (3),
-- porque ese filtra de nuevo por id_usuario sobre el estado mas reciente. El
-- resultado: una sesion se elimina de la base sin haberse revocado nunca en
-- Redis. Su JWT sigue siendo valido hasta que expire por si solo, y ya no
-- queda ninguna fila que permita rastrearlo (lectura no repetible).
--
-- Por que esta forma lo corrige: DELETE ... RETURNING lee y borra en UNA sola
-- sentencia sobre UN solo snapshot. No existe "entre (1) y (3)" porque no hay
-- dos pasos: es imposible que una sesion se cuele sin ser devuelta, porque
-- toda fila que la sentencia borra es, por definicion, una fila que tambien
-- devuelve.
--
-- La escritura en Redis permanece deliberadamente en Java (SessionRevocationService):
-- Redis no participa en la transaccion de PostgreSQL, y un fallo suyo no debe
-- revertir el borrado en la base (ni al reves). Lo que esta funcion garantiza
-- es que Java recibe EXACTAMENTE el conjunto de jti que se borro, ni uno mas
-- ni uno menos.
--
-- segundos_restantes se calcula en el mismo SELECT para que Java no tenga que
-- releer fecha_expiracion por separado; GREATEST(0, ...) evita valores
-- negativos si la sesion ya habia expirado.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_revocar_sesiones_usuario(
    p_id_usuario BIGINT
)
RETURNS TABLE (jti VARCHAR(36), segundos_restantes INTEGER)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_id_usuario IS NULL THEN
        RAISE EXCEPTION 'fn_revocar_sesiones_usuario: p_id_usuario es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    RETURN QUERY
    DELETE FROM sesiones_usuario s
     WHERE s.id_usuario = p_id_usuario
    RETURNING s.jti,
              GREATEST(0, EXTRACT(EPOCH FROM (s.fecha_expiracion - CURRENT_TIMESTAMP))::INTEGER);
END;
$$;

COMMENT ON FUNCTION fn_revocar_sesiones_usuario(BIGINT)
    IS 'Fase 1 concurrencia - DELETE ... RETURNING atomico: lee y borra las sesiones de un usuario en una sola sentencia, eliminando la ventana de lectura no repetible entre leer y borrar por separado.';
