-- =============================================================================
-- fn_resolver_estado_login
-- Categoria funcional: consultas multi-tabla                          Requisito: REQ-F-002
-- =============================================================================
-- Resuelve, en una sola llamada, todo lo que AuthServiceImpl.login/verify2Fa
-- necesitan del usuario tras validar la contrasena con el AuthenticationManager
-- de Spring Security: datos basicos de la cuenta, si el 2FA esta habilitado y
-- la lista de roles asignados (join usuarios - usuario_roles - roles).
--
-- Por que en el motor y no en Java: la version anterior hacia tres consultas
-- independientes (findByCorreo, findByUsuarioIdUsuario en 2FA,
-- findByUsuarioIdUsuario en usuario_roles) en tres idas y vueltas separadas a
-- la base. Aqui se resuelven en una sola sentencia con dos LEFT JOIN y una
-- agregacion de roles, evitando el problema N+1 y garantizando una lectura
-- consistente de los tres estados a la vez.
--
-- No participa en la validacion de la contrasena en si: eso permanece en
-- AuthenticationManager (BCrypt vive fuera del motor de datos). Esta funcion
-- solo se invoca DESPUES de que la autenticacion por contrasena ya tuvo exito.
--
-- Devuelve JSONB con la forma:
--   { idUsuario, correo, nombres, apellidos, estadoCuenta,
--     dosFactoresHabilitado, roles: [ "CLIENTE", ... ] }
-- Devuelve NULL si el correo no existe (la capa de servicio lo traduce a 404).
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_resolver_estado_login(
    p_correo VARCHAR(150)
)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_resultado JSONB;
BEGIN
    IF p_correo IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT jsonb_build_object(
               'idUsuario', u.id_usuario,
               'correo', u.correo,
               'nombres', u.nombres,
               'apellidos', u.apellidos,
               'estadoCuenta', u.estado_cuenta,
               'dosFactoresHabilitado', COALESCE(tf.esta_habilitado, FALSE),
               'roles', COALESCE(
                            (SELECT jsonb_agg(r.nombre_rol ORDER BY r.nombre_rol)
                               FROM usuario_roles ur
                               JOIN roles r ON r.id_rol = ur.id_rol
                              WHERE ur.id_usuario = u.id_usuario),
                            '[]'::jsonb)
           )
      INTO v_resultado
      FROM usuarios u
      LEFT JOIN autenticacion_dos_factores tf ON tf.id_usuario = u.id_usuario
     WHERE u.correo = p_correo;

    RETURN v_resultado;
END;
$$;

COMMENT ON FUNCTION fn_resolver_estado_login(VARCHAR)
    IS 'REQ-F-002 - Consulta multi-tabla: resuelve estado de cuenta, 2FA y roles de un usuario en una sola llamada para el flujo de login.';
