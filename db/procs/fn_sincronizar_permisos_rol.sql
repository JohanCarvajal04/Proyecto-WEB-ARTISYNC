-- =============================================================================
-- fn_sincronizar_permisos_rol
-- Categoria funcional: actualizaciones masivas                        Requisito: REQ-F-003
-- =============================================================================
-- Reemplaza, en una sola transaccion, el conjunto completo de permisos
-- asignados a un rol por el conjunto recibido en p_codigos_permiso.
-- Sustituye a RolePermissionServiceImpl.syncPermissions, que cargaba el rol
-- completo con sus permisos (fetch EAGER de la coleccion @ManyToMany),
-- resolvia cada codigo de permiso con una consulta individual y dejaba que
-- Hibernate calculara el diff de la coleccion al hacer save().
--
-- Por que en el motor y no en Java: el reemplazo de un set completo es una
-- operacion tipicamente DELETE+INSERT sobre la tabla puente rol_permisos; con
-- lote de permisos y necesitando validar cada codigo antes de aplicar nada
-- (todo o nada), es mas seguro y mas barato en round-trips resolverlo como una
-- unica sentencia DELETE seguida de un INSERT ... SELECT que hacerlo fila por
-- fila desde el servicio.
--
-- Valida cada codigo de permiso contra la tabla permisos antes de aplicar el
-- reemplazo: si algun codigo no existe, no se modifica nada (rollback
-- implicito de la transaccion de la funcion).
--
-- Devuelve el numero de permisos finalmente asignados al rol.
-- Lanza excepcion si el rol no existe o si algun codigo de permiso es invalido.
--
-- Seguridad: el arreglo de codigos llega como parametro tipado
-- (TEXT[]); no hay concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_sincronizar_permisos_rol(
    p_nombre_rol       VARCHAR(50),
    p_codigos_permiso  TEXT[]
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_rol           BIGINT;
    v_codigos_normalizados TEXT[];
    v_encontrados      INTEGER;
    v_esperados        INTEGER;
    v_total_asignado   INTEGER;
BEGIN
    IF p_nombre_rol IS NULL THEN
        RAISE EXCEPTION 'fn_sincronizar_permisos_rol: p_nombre_rol es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    SELECT id_rol INTO v_id_rol FROM roles WHERE nombre_rol = UPPER(p_nombre_rol);
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Rol no encontrado: %', p_nombre_rol
            USING ERRCODE = 'P0002';
    END IF;

    IF p_codigos_permiso IS NULL THEN
        v_codigos_normalizados := ARRAY[]::TEXT[];
    ELSE
        SELECT array_agg(DISTINCT UPPER(codigo)) INTO v_codigos_normalizados
          FROM unnest(p_codigos_permiso) AS codigo;
    END IF;

    v_esperados := COALESCE(array_length(v_codigos_normalizados, 1), 0);

    IF v_esperados > 0 THEN
        SELECT COUNT(*) INTO v_encontrados
          FROM permisos
         WHERE nombre_permiso = ANY (v_codigos_normalizados);

        IF v_encontrados <> v_esperados THEN
            RAISE EXCEPTION 'Uno o mas permisos son inexistentes para el rol %', p_nombre_rol
                USING ERRCODE = '23503';
        END IF;
    END IF;

    DELETE FROM rol_permisos WHERE id_rol = v_id_rol;

    INSERT INTO rol_permisos (id_rol, id_permiso)
    SELECT v_id_rol, p.id_permiso
      FROM permisos p
     WHERE p.nombre_permiso = ANY (v_codigos_normalizados);

    GET DIAGNOSTICS v_total_asignado = ROW_COUNT;

    RETURN v_total_asignado;
END;
$$;

COMMENT ON FUNCTION fn_sincronizar_permisos_rol(VARCHAR, TEXT[])
    IS 'REQ-F-003 - Actualizacion masiva: reemplaza atomicamente el conjunto de permisos de un rol (DELETE+INSERT en rol_permisos), validando cada codigo antes de aplicar el cambio.';
