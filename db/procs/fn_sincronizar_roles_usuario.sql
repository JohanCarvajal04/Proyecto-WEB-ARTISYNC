-- =============================================================================
-- fn_sincronizar_roles_usuario
-- Categoria funcional: actualizaciones masivas                  Requisito: REQ-NF (concurrencia)
-- Fase 1 de docs/basedatos/PLAN-CONCURRENCIA-SP.md — corrige la anomalia A2.
-- =============================================================================
-- Reemplaza atomicamente el conjunto completo de roles de un usuario. Gemela
-- de fn_sincronizar_permisos_rol (REQ-F-003), que ya resolvio este mismo
-- patron para roles<->permisos.
--
-- Sustituye a AdminUserServiceImpl.actualizarRoles(): findByUsuarioIdUsuario +
-- deleteAll + flush + POR CADA rol nuevo (findByNombreRol + save + consulta
-- de perfil de creador + save de perfil) -- unos 10 viajes a la base sin
-- ninguna atomicidad entre ellos.
--
-- Dos anomalias que corrige:
--
--   * Lectura fantasma: sin restriccion unica, dos administradores editando
--     el mismo usuario a la vez podian dejar roles duplicados en
--     usuario_roles. Cerrada de forma ESTRUCTURAL por uq_usuario_rol
--     (V17__concurrencia_seguridad.sql) + ON CONFLICT DO NOTHING: el fantasma
--     es imposible aunque otra transaccion se adelante entre el DELETE y el
--     INSERT de esta misma rutina.
--
--   * Actualizacion perdida / estado a medias: SELECT ... FOR UPDATE sobre la
--     fila de usuarios serializa dos sincronizaciones concurrentes del MISMO
--     usuario (el segundo escritor espera y ve el resultado ya confirmado del
--     primero, no un estado intermedio). Ademas, TODOS los roles nuevos se
--     validan ANTES de borrar los antiguos: si un nombre de rol no existe, se
--     aborta sin haber dejado al usuario sin ningun rol (la version en Java
--     borraba primero y podia fallar a mitad del bucle de alta).
--
-- Orden de bloqueo: usuarios -> usuario_roles -> roles (convencion canonica
-- del modulo, ver docs/basedatos/PLAN-CONCURRENCIA-SP.md §0.5.4), evita
-- interbloqueos con fn_sincronizar_permisos_rol y fn_eliminar_rol.
--
-- Devuelve el total de filas de usuario_roles insertadas. Lanza excepcion si
-- el usuario no existe, si el array de roles es nulo/vacio, o si alguno de
-- los nombres de rol no existe en el sistema.
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_sincronizar_roles_usuario(
    p_id_usuario  BIGINT,
    p_nombres_rol TEXT[]
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_existe     BOOLEAN;
    v_nombre_rol TEXT;
    v_total      INTEGER := 0;
BEGIN
    IF p_id_usuario IS NULL THEN
        RAISE EXCEPTION 'fn_sincronizar_roles_usuario: p_id_usuario es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    IF p_nombres_rol IS NULL OR array_length(p_nombres_rol, 1) IS NULL THEN
        RAISE EXCEPTION 'fn_sincronizar_roles_usuario: se requiere al menos un rol'
            USING ERRCODE = '22004';
    END IF;

    -- Bloqueo del agregado raiz: serializa sincronizaciones concurrentes del
    -- mismo usuario y evita que este SELECT vea un conjunto de roles que otra
    -- transaccion cambia justo despues (lectura no repetible).
    SELECT TRUE INTO v_existe
      FROM usuarios
     WHERE id_usuario = p_id_usuario
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Usuario no encontrado con ID: %', p_id_usuario
            USING ERRCODE = 'P0002';
    END IF;

    -- Validacion completa antes de tocar usuario_roles: aborta limpio (todo o
    -- nada) si algun nombre de rol no existe.
    FOREACH v_nombre_rol IN ARRAY p_nombres_rol LOOP
        IF NOT EXISTS (SELECT 1 FROM roles WHERE UPPER(nombre_rol) = UPPER(v_nombre_rol)) THEN
            RAISE EXCEPTION 'El rol especificado no existe en el sistema: %', UPPER(v_nombre_rol)
                USING ERRCODE = '23514';
        END IF;
    END LOOP;

    DELETE FROM usuario_roles WHERE id_usuario = p_id_usuario;

    -- ON CONFLICT DO NOTHING: idempotente aunque el array traiga nombres
    -- repetidos, y ultima linea de defensa estructural contra el fantasma si
    -- alguna otra via insertara sobre esta misma pareja (usuario, rol).
    INSERT INTO usuario_roles (id_usuario, id_rol)
    SELECT p_id_usuario, r.id_rol
      FROM unnest(p_nombres_rol) AS n(nombre)
      JOIN roles r ON UPPER(r.nombre_rol) = UPPER(n.nombre)
    ON CONFLICT (id_usuario, id_rol) DO NOTHING;

    GET DIAGNOSTICS v_total = ROW_COUNT;

    -- Alta perezosa del perfil de creador (mismo criterio que
    -- AdminUserServiceImpl.actualizarRoles ya aplicaba), tambien idempotente.
    IF EXISTS (SELECT 1 FROM unnest(p_nombres_rol) AS n(nombre) WHERE UPPER(n.nombre) = 'CREADOR') THEN
        INSERT INTO perfiles_creadores (id_usuario, biografia)
        SELECT p_id_usuario, 'Hola! Soy un creador en ARTISYNC.'
         WHERE NOT EXISTS (SELECT 1 FROM perfiles_creadores WHERE id_usuario = p_id_usuario);
    END IF;

    RETURN v_total;
END;
$$;

COMMENT ON FUNCTION fn_sincronizar_roles_usuario(BIGINT, TEXT[])
    IS 'Fase 1 concurrencia - Reemplaza atomicamente el conjunto de roles de un usuario (DELETE+INSERT con ON CONFLICT), serializado con SELECT FOR UPDATE sobre usuarios; cierra lectura fantasma y estados a medias.';
