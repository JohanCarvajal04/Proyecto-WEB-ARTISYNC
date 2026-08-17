-- =============================================================================
-- fn_eliminar_rol
-- Categoria funcional: validaciones cruzadas                          Requisito: REQ-F-004
-- =============================================================================
-- Elimina un rol personalizado tras comprobar, dentro de la misma transaccion,
-- dos condiciones de negocio: que no sea uno de los roles base protegidos del
-- sistema, y que no tenga usuarios activos asignados (usuario_roles).
-- Sustituye a RolePermissionServiceImpl.deleteRole, que cargaba el rol
-- completo, validaba en Java contra un Set<String> de roles protegidos y
-- consultaba usuarioRolRepository.existsByRolIdRol antes de delete().
--
-- Por que en el motor: la comprobacion "sin usuarios asignados" debe ser
-- atomica frente al DELETE que la sigue -- si un UsuarioRol se insertara entre
-- la validacion y el borrado, se perderia la asociacion silenciosamente. Al
-- resolverlo como una unica sentencia dentro de la funcion, la validacion y el
-- borrado comparten la misma transaccion.
--
-- Devuelve TRUE si elimino el rol. Lanza excepcion si el rol no existe, si es
-- un rol base protegido, o si tiene usuarios activos asignados.
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_eliminar_rol(
    p_id_rol BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_nombre_rol VARCHAR(50);
    v_usuarios_asignados INTEGER;
    v_roles_protegidos TEXT[] := ARRAY['ADMIN', 'CLIENTE', 'CREADOR', 'MODERADOR', 'SOPORTE', 'AUDITOR_FINANCIERO'];
BEGIN
    IF p_id_rol IS NULL THEN
        RAISE EXCEPTION 'fn_eliminar_rol: p_id_rol es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    SELECT nombre_rol INTO v_nombre_rol FROM roles WHERE id_rol = p_id_rol;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Rol no encontrado con ID: %', p_id_rol
            USING ERRCODE = 'P0002';
    END IF;

    IF UPPER(v_nombre_rol) = ANY (v_roles_protegidos) THEN
        RAISE EXCEPTION 'No se puede eliminar un rol base del sistema: %', v_nombre_rol
            USING ERRCODE = '23514';
    END IF;

    SELECT COUNT(*) INTO v_usuarios_asignados
      FROM usuario_roles
     WHERE id_rol = p_id_rol;

    IF v_usuarios_asignados > 0 THEN
        RAISE EXCEPTION 'No se puede eliminar el rol porque tiene usuarios activos asignados: %', v_nombre_rol
            USING ERRCODE = '23514';
    END IF;

    DELETE FROM rol_permisos WHERE id_rol = p_id_rol;
    DELETE FROM roles WHERE id_rol = p_id_rol;

    RETURN TRUE;
END;
$$;

COMMENT ON FUNCTION fn_eliminar_rol(BIGINT)
    IS 'REQ-F-004 - Validacion cruzada: elimina un rol personalizado solo si no es un rol base protegido y no tiene usuarios asignados.';
