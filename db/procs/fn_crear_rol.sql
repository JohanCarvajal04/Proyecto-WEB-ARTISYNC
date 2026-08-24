-- =============================================================================
-- fn_crear_rol
-- Categoria funcional: validaciones cruzadas                    Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §4 — corrige la anomalia A8.
-- =============================================================================
-- Crea un rol personalizado y le asigna sus permisos iniciales, en una unica
-- transaccion.
--
-- Sustituye a RolePermissionServiceImpl.createRole, que comprobaba
-- rolRepository.findByNombreRol(...).isPresent() y luego hacia save() en
-- sentencias separadas -- lectura fantasma no atomica: entre la comprobacion
-- y el insert, otra transaccion podia crear un rol con el mismo nombre.
-- Mitigado en la practica por roles.nombre_rol UNIQUE, pero sin traduccion de
-- error (500 crudo en vez de 409).
--
-- Captura la violacion de unicidad con un bloque EXCEPTION (mismo molde que
-- fn_crear_usuario_admin) en vez de una comprobacion previa: PostgreSQL no
-- ofrece bloqueo de rango bajo READ COMMITTED, asi que la restriccion UNIQUE
-- como predicado es la unica defensa correcta.
--
-- Delega la asignacion de permisos iniciales en fn_sincronizar_permisos_rol
-- (REQ-F-003, ya existente): evita reimplementar la validacion de codigos de
-- permiso y garantiza el mismo comportamiento que syncPermissions.
--
-- Devuelve el id_rol generado. Lanza excepcion si el nombre ya existe, o si
-- algun codigo de permiso inicial es invalido (via fn_sincronizar_permisos_rol).
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_crear_rol(
    p_nombre_rol       VARCHAR(50),
    p_descripcion_rol  TEXT,
    p_codigos_permiso  TEXT[]
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_rol         BIGINT;
    v_nombre_rol_norm VARCHAR(50);
BEGIN
    IF p_nombre_rol IS NULL OR btrim(p_nombre_rol) = '' THEN
        RAISE EXCEPTION 'fn_crear_rol: p_nombre_rol es obligatorio'
            USING ERRCODE = '22004';
    END IF;

    v_nombre_rol_norm := UPPER(btrim(p_nombre_rol));

    -- Subtransaccion explicita: el bloque EXCEPTION abre un SAVEPOINT
    -- implicito. Si el INSERT viola uq roles.nombre_rol (fantasma
    -- materializado por otra transaccion concurrente), se hace ROLLBACK TO
    -- SAVEPOINT automatico y se traduce a 409 en vez de un 500 crudo.
    BEGIN
        INSERT INTO roles (nombre_rol, descripcion_rol)
        VALUES (v_nombre_rol_norm, p_descripcion_rol)
        RETURNING id_rol INTO v_id_rol;
    EXCEPTION
        WHEN unique_violation THEN
            RAISE EXCEPTION 'Ya existe un rol con el nombre: %', v_nombre_rol_norm
                USING ERRCODE = '23505';
    END;

    IF p_codigos_permiso IS NOT NULL AND array_length(p_codigos_permiso, 1) > 0 THEN
        PERFORM fn_sincronizar_permisos_rol(v_nombre_rol_norm, p_codigos_permiso);
    END IF;

    RETURN v_id_rol;
END;
$$;

COMMENT ON FUNCTION fn_crear_rol(VARCHAR, TEXT, TEXT[])
    IS 'Fase 3 concurrencia - Crea un rol y asigna sus permisos iniciales atomicamente, capturando unique_violation en vez de una comprobacion findByNombreRol no atomica (A8).';
