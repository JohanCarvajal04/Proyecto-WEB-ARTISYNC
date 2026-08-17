-- =============================================================================
-- fn_registrar_usuario
-- Categoria funcional: validaciones cruzadas + insercion multi-tabla   Requisito: REQ-F-001
-- =============================================================================
-- Registra un nuevo usuario de la plataforma en una unica transaccion atomica:
-- valida que el correo no exista, valida mayoria de edad (RNF-12, >= 18 anios),
-- valida que el rol solicitado sea uno de los permitidos en auto-registro
-- (CLIENTE o CREADOR), inserta la fila en usuarios, la asociacion en
-- usuario_roles y, si el rol es CREADOR, el perfil de creador inicial.
--
-- Por que en el motor y no en AuthServiceImpl.register: son cuatro lecturas/
-- escrituras (existsByCorreo, findByNombreRol, insert usuario, insert
-- usuario_roles, insert perfil opcional) que deben ser atomicas: si el perfil
-- de creador fallara tras crear el usuario, quedaria una cuenta sin perfil.
-- Resolverlo en el motor evita el patron de multiples idas y vueltas y las
-- inconsistencias parciales que un rollback incompleto en Java podria dejar.
--
-- El hash de la contrasena se calcula en Java (BCrypt vive fuera del motor de
-- datos) y llega ya cifrado como parametro; la funcion nunca ve la contrasena
-- en texto plano.
--
-- Devuelve el id_usuario generado. Lanza excepcion si el correo ya existe, si
-- la fecha de nacimiento no cumple la mayoria de edad, o si el rol solicitado
-- no existe o no esta permitido en auto-registro.
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_registrar_usuario(
    p_nombres           VARCHAR(100),
    p_apellidos         VARCHAR(100),
    p_correo            VARCHAR(150),
    p_contrasena_hash   VARCHAR(255),
    p_fecha_nacimiento  DATE,
    p_nombre_rol        VARCHAR(50)
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_usuario BIGINT;
    v_id_rol     BIGINT;
    v_nombre_rol VARCHAR(50) := UPPER(p_nombre_rol);
BEGIN
    IF p_correo IS NULL OR p_contrasena_hash IS NULL OR p_fecha_nacimiento IS NULL THEN
        RAISE EXCEPTION 'fn_registrar_usuario: correo, contrasena y fecha de nacimiento son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    IF EXISTS (SELECT 1 FROM usuarios WHERE correo = p_correo) THEN
        RAISE EXCEPTION 'El correo % ya esta registrado en la plataforma', p_correo
            USING ERRCODE = '23505';
    END IF;

    -- RNF-12: mayoria de edad (>= 18 anios cumplidos a la fecha actual).
    IF p_fecha_nacimiento > (CURRENT_DATE - INTERVAL '18 years')::date THEN
        RAISE EXCEPTION 'Debes tener al menos 18 anios para registrarte en ARTISYNC (RNF-12)'
            USING ERRCODE = '23514';
    END IF;

    IF v_nombre_rol NOT IN ('CLIENTE', 'CREADOR') THEN
        RAISE EXCEPTION 'Rol no permitido en registro. Solo se permiten CLIENTE o CREADOR: %', p_nombre_rol
            USING ERRCODE = '23514';
    END IF;

    SELECT id_rol INTO v_id_rol FROM roles WHERE nombre_rol = v_nombre_rol;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'El rol especificado no existe en el sistema: %', v_nombre_rol
            USING ERRCODE = '23503';
    END IF;

    INSERT INTO usuarios (nombres, apellidos, correo, contrasena_hash, fecha_nacimiento, estado_cuenta, fecha_registro)
    VALUES (p_nombres, p_apellidos, p_correo, p_contrasena_hash, p_fecha_nacimiento, TRUE, CURRENT_TIMESTAMP)
    RETURNING id_usuario INTO v_id_usuario;

    INSERT INTO usuario_roles (id_usuario, id_rol)
    VALUES (v_id_usuario, v_id_rol);

    IF v_nombre_rol = 'CREADOR' THEN
        INSERT INTO perfiles_creadores (id_usuario, biografia)
        VALUES (v_id_usuario, 'Hola! Soy un creador en ARTISYNC.');
    END IF;

    RETURN v_id_usuario;
END;
$$;

COMMENT ON FUNCTION fn_registrar_usuario(VARCHAR, VARCHAR, VARCHAR, VARCHAR, DATE, VARCHAR)
    IS 'REQ-F-001 - Insercion multi-tabla: registra usuario + usuario_roles + perfil de creador opcional, validando correo unico, mayoria de edad y rol permitido.';
