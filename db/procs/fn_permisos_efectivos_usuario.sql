-- =============================================================================
-- fn_permisos_efectivos_usuario
-- Categoria funcional: consultas multi-tabla                    Requisito: REQ-NF (rendimiento)
-- Fase 2 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §8.
-- =============================================================================
-- Resuelve, en una sola llamada, todo lo que CustomUserDetailsService.loadUserByUsername
-- necesita para autenticar una peticion: datos basicos de la cuenta y el
-- conjunto completo de authorities de Spring Security (roles con prefijo
-- ROLE_ + permisos), ya deduplicado.
--
-- Por que en el motor: loadUserByUsername se ejecuta EN CADA peticion
-- autenticada (via JwtAuthenticationFilter), y hacia: findByCorreo (1) +
-- findByUsuarioIdUsuario en usuario_roles (1) + por cada rol, el acceso a
-- Rol.permisos (FetchType.EAGER) resuelto con un SELECT propio (N) -- entre 4
-- y 8 consultas por peticion segun cuantos roles tenga el usuario, con el N+1
-- clasico. Aqui se resuelve con dos subconsultas (UNION, deduplicadas por
-- DISTINCT dentro del jsonb_agg) sobre usuario_roles/roles/rol_permisos/permisos.
--
-- STABLE (no LANGUAGE plpgsql con side effects, no escribe nada): dentro de
-- una misma llamada a la funcion, PostgreSQL evalua todas las subconsultas
-- sobre el MISMO snapshot, asi que roles y permisos siempre son coherentes
-- entre si -- a diferencia de las 2-3 consultas independientes que sustituye,
-- que bajo READ COMMITTED podian ver una version de los roles y otra de los
-- permisos si una sincronizacion (fn_sincronizar_roles_usuario,
-- fn_sincronizar_permisos_rol) se colaba justo entremedias.
--
-- La contrasena_hash SI viaja en el JSONB (a diferencia de fn_resolver_estado_login,
-- que no la necesita): loadUserByUsername construye un UserDetails completo, y
-- el AuthenticationManager de Spring Security compara el hash BCrypt fuera del
-- motor. El valor nunca se loguea ni se expone en ninguna respuesta HTTP.
--
-- Devuelve NULL si el correo no existe (la capa Java lo traduce a
-- UsernameNotFoundException, igual que antes).
--
-- Seguridad: parametro formal tipado; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_permisos_efectivos_usuario(
    p_correo VARCHAR(150)
)
RETURNS JSONB
LANGUAGE sql
STABLE
AS $$
    SELECT jsonb_build_object(
               'idUsuario', u.id_usuario,
               'correo', u.correo,
               'contrasenaHash', u.contrasena_hash,
               'estadoCuenta', u.estado_cuenta,
               'authorities', COALESCE(
                   (SELECT jsonb_agg(DISTINCT a.autoridad)
                      FROM (
                            SELECT 'ROLE_' || UPPER(r.nombre_rol) AS autoridad
                              FROM usuario_roles ur
                              JOIN roles r ON r.id_rol = ur.id_rol
                             WHERE ur.id_usuario = u.id_usuario
                            UNION
                            SELECT UPPER(p.nombre_permiso)
                              FROM usuario_roles ur
                              JOIN rol_permisos rp ON rp.id_rol = ur.id_rol
                              JOIN permisos p ON p.id_permiso = rp.id_permiso
                             WHERE ur.id_usuario = u.id_usuario
                           ) a),
                   '[]'::jsonb)
           )
      FROM usuarios u
     WHERE u.correo = p_correo;
$$;

COMMENT ON FUNCTION fn_permisos_efectivos_usuario(VARCHAR)
    IS 'Fase 2 rendimiento - Resuelve usuario + authorities (roles ROLE_* y permisos) en una sola llamada STABLE, sustituyendo el N+1 de CustomUserDetailsService en cada peticion autenticada.';
