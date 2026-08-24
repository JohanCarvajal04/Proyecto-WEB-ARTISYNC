-- =============================================================================
-- fn_crear_usuario_admin
-- Categoria funcional: validaciones cruzadas + inserción multi-tabla   Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §4 — corrige la anomalia A3.
-- =============================================================================
-- Crea un usuario desde el panel administrativo, con su(s) rol(es) y el
-- perfil de creador si corresponde, en una unica transaccion.
--
-- Sustituye a AdminUserServiceImpl.createUser, que comprobaba
-- existsByCorreo(...) y luego hacia save() en sentencias separadas -- una
-- comprobacion "existe?" no atomica respecto a la insercion (lectura
-- fantasma): entre ambas, otra transaccion podia insertar el mismo correo.
-- PostgreSQL no ofrece bloqueo de rango bajo READ COMMITTED ni REPEATABLE READ
-- (no se puede "bloquear un correo que aun no existe"), asi que la unica
-- defensa correcta es la restriccion UNIQUE usuarios.correo como predicado,
-- capturada aqui con un bloque EXCEPTION en vez de una comprobacion previa.
-- En el peor caso el correo duplicado terminaba en un 500 crudo en vez del
-- 409 CONFLICT esperado (el fantasma SI quedaba bloqueado por el UNIQUE, pero
-- sin traduccion de error).
--
-- Delega la asignacion de roles (y el alta perezosa de perfiles_creadores) en
-- fn_sincronizar_roles_usuario (Fase 1, docs/basedatos/PLAN-CONCURRENCIA-SP.md
-- §3): evita reimplementar esa logica y garantiza el mismo comportamiento que
-- assignRoles/updateUser para el caso "rol CREADOR incluido".
--
-- Devuelve el id_usuario generado. Lanza excepcion si el correo ya existe, si
-- el pais no existe, o si algun rol solicitado no existe (via
-- fn_sincronizar_roles_usuario).
--
-- Seguridad: parametros formales tipados (la contrasena llega ya hasheada con
-- BCrypt, calculada en Java); sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_crear_usuario_admin(
    p_nombres          VARCHAR(100),
    p_apellidos        VARCHAR(100),
    p_correo           VARCHAR(150),
    p_contrasena_hash  VARCHAR(255),
    p_fecha_nacimiento DATE,
    p_id_pais          BIGINT,
    p_estado_cuenta    BOOLEAN,
    p_nombres_rol      TEXT[]
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_usuario BIGINT;
BEGIN
    IF p_correo IS NULL OR p_contrasena_hash IS NULL THEN
        RAISE EXCEPTION 'fn_crear_usuario_admin: p_correo y p_contrasena_hash son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    IF p_id_pais IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pais WHERE id_pais = p_id_pais) THEN
        RAISE EXCEPTION 'Pais no encontrado' USING ERRCODE = '23503';
    END IF;

    -- Subtransaccion explicita: el bloque EXCEPTION abre un SAVEPOINT
    -- implicito. Si el INSERT viola uq usuarios.correo (fantasma
    -- materializado por otra transaccion concurrente), se hace ROLLBACK TO
    -- SAVEPOINT automatico y se traduce a 409 en vez de un 500 crudo.
    BEGIN
        INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash,
                               fecha_nacimiento, id_pais, estado_cuenta)
        VALUES (p_nombres, p_apellidos, p_correo, p_contrasena_hash,
                p_fecha_nacimiento, p_id_pais, COALESCE(p_estado_cuenta, TRUE))
        RETURNING id_usuario INTO v_id_usuario;
    EXCEPTION
        WHEN unique_violation THEN
            RAISE EXCEPTION 'El correo ya esta registrado: %', p_correo
                USING ERRCODE = '23505';
    END;

    PERFORM fn_sincronizar_roles_usuario(
        v_id_usuario,
        COALESCE(NULLIF(p_nombres_rol, ARRAY[]::TEXT[]), ARRAY['CLIENTE']));

    RETURN v_id_usuario;
END;
$$;

COMMENT ON FUNCTION fn_crear_usuario_admin(VARCHAR, VARCHAR, VARCHAR, VARCHAR, DATE, BIGINT, BOOLEAN, TEXT[])
    IS 'Fase 3 concurrencia - Crea un usuario administrativo con sus roles en una transaccion atomica, capturando unique_violation en vez de una comprobacion existsByCorreo no atomica (A3).';
