-- =============================================================================
-- fn_restablecer_contrasena
-- Categoria funcional: validaciones cruzadas + escritura multi-tabla  Requisito: REQ-F-005
-- =============================================================================
-- Aplica un restablecimiento de contrasena a partir de un token de
-- recuperacion: valida que el token exista, no haya sido usado y no haya
-- expirado (ventana de 60 minutos desde su generacion), y en tal caso
-- actualiza el hash de la contrasena del usuario y marca el token como usado,
-- en una unica transaccion atomica.
-- Sustituye a AuthServiceImpl.resetPassword, que hacia la busqueda del token,
-- la validacion de expiracion en Java (LocalDateTime.plusMinutes(60)) y dos
-- save() secuenciales (usuario, tokenRecuperacion).
--
-- Por que en el motor: entre la validacion del token y su marcado como usado
-- no debe existir ventana en la que una segunda peticion concurrente con el
-- mismo token pueda colarse y restablecer la contrasena dos veces. La fila del
-- token se toma con FOR UPDATE para serializar restablecimientos concurrentes
-- del mismo token.
--
-- El nuevo hash de contrasena se calcula en Java (BCrypt) y llega ya cifrado;
-- la funcion nunca ve la contrasena en texto plano. El hash del token en si
-- (SHA-256 del valor plano enviado por correo) tambien se calcula en Java
-- antes de invocar la funcion.
--
-- Devuelve el id_usuario cuya contrasena se actualizo. Lanza excepcion si el
-- token no existe, ya fue usado, o expiro.
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_restablecer_contrasena(
    p_hash_token           VARCHAR(255),
    p_nueva_contrasena_hash VARCHAR(255)
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_token   BIGINT;
    v_id_usuario BIGINT;
    v_fecha_generacion TIMESTAMP;
BEGIN
    IF p_hash_token IS NULL OR p_nueva_contrasena_hash IS NULL THEN
        RAISE EXCEPTION 'fn_restablecer_contrasena: hash_token y nueva_contrasena_hash son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    SELECT id_token, id_usuario, fecha_generacion
      INTO v_id_token, v_id_usuario, v_fecha_generacion
      FROM tokens_recuperacion
     WHERE hash_token = p_hash_token
       AND usado = FALSE
       FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Este enlace ya ha sido utilizado o ha expirado'
            USING ERRCODE = '23514';
    END IF;

    IF v_fecha_generacion + INTERVAL '60 minutes' < CURRENT_TIMESTAMP THEN
        RAISE EXCEPTION 'Este enlace ya ha sido utilizado o ha expirado'
            USING ERRCODE = '23514';
    END IF;

    UPDATE usuarios
       SET contrasena_hash = p_nueva_contrasena_hash
     WHERE id_usuario = v_id_usuario;

    UPDATE tokens_recuperacion
       SET usado = TRUE
     WHERE id_token = v_id_token;

    RETURN v_id_usuario;
END;
$$;

COMMENT ON FUNCTION fn_restablecer_contrasena(VARCHAR, VARCHAR)
    IS 'REQ-F-005 - Validacion cruzada + escritura multi-tabla: valida token de recuperacion (no usado, no expirado) y actualiza usuarios + tokens_recuperacion atomicamente.';
