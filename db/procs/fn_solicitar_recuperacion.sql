-- =============================================================================
-- fn_solicitar_recuperacion
-- Categoria funcional: validaciones cruzadas + escritura multi-tabla   Requisito: REQ-NF (concurrencia)
-- Fase 3 de docs/basedatos/PLAN-CONCURRENCIA-SP.md §6 — corrige la anomalia A5.
-- =============================================================================
-- Invalida los tokens de recuperacion previos de un usuario e inserta el
-- nuevo, en una unica transaccion.
--
-- Sustituye a AuthServiceImpl.forgotPassword (parte de escritura), que
-- insertaba un TokenRecuperacion mas sin tocar los anteriores: cada solicitud
-- de "olvide mi contrasena" dejaba un token adicional valido durante 60
-- minutos (ventana de fn_restablecer_contrasena, REQ-F-005), de modo que un
-- usuario que pedia varios enlaces de recuperacion acumulaba N tokens
-- utilizables simultaneamente -- superficie de ataque innecesaria si alguno se
-- filtraba (log, proxy, bandeja de entrada comprometida).
--
-- SELECT ... FOR UPDATE sobre la fila de usuarios serializa solicitudes
-- concurrentes del mismo correo (p. ej. un usuario haciendo doble clic en
-- "reenviar enlace"): la segunda espera a que la primera confirme su
-- invalidacion+insercion antes de repetir el mismo patron, en vez de que
-- ambas lean "no hay token que invalidar" y dejen dos tokens validos.
--
-- Preserva DELIBERADAMENTE el comportamiento de "respuesta indistinguible" de
-- forgotPassword: devuelve NULL (no lanza excepcion) si el correo no existe o
-- la cuenta esta inactiva, para que la capa Java responda el mismo mensaje
-- gener ico exista o no la cuenta (no revelar informacion de la cuenta).
--
-- El nuevo hash de token (SHA-256 del valor plano enviado por correo) se
-- calcula en Java antes de invocar esta funcion, igual que en
-- fn_restablecer_contrasena; la funcion nunca ve el token en texto plano.
--
-- Devuelve JSONB {idUsuario, nombres} si la cuenta existe y esta activa, o
-- NULL en caso contrario.
--
-- Seguridad: parametros formales tipados; sin concatenacion ni EXECUTE.
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_solicitar_recuperacion(
    p_correo     VARCHAR(150),
    p_hash_token VARCHAR(255)
)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
    v_id_usuario BIGINT;
    v_nombres    VARCHAR(100);
BEGIN
    IF p_correo IS NULL OR p_hash_token IS NULL THEN
        RAISE EXCEPTION 'fn_solicitar_recuperacion: p_correo y p_hash_token son obligatorios'
            USING ERRCODE = '22004';
    END IF;

    SELECT id_usuario, nombres INTO v_id_usuario, v_nombres
      FROM usuarios
     WHERE correo = p_correo
       AND estado_cuenta = TRUE
       FOR UPDATE;

    IF NOT FOUND THEN
        RETURN NULL;
    END IF;

    -- Invalida cualquier token previo aun vigente antes de insertar el nuevo:
    -- a lo sumo un token de recuperacion vigente por usuario en todo momento.
    UPDATE tokens_recuperacion
       SET usado = TRUE
     WHERE id_usuario = v_id_usuario
       AND usado = FALSE;

    INSERT INTO tokens_recuperacion (id_usuario, hash_token, usado)
    VALUES (v_id_usuario, p_hash_token, FALSE);

    RETURN jsonb_build_object('idUsuario', v_id_usuario, 'nombres', v_nombres);
END;
$$;

COMMENT ON FUNCTION fn_solicitar_recuperacion(VARCHAR, VARCHAR)
    IS 'Fase 3 concurrencia - Invalida tokens de recuperacion previos e inserta el nuevo atomicamente bajo SELECT FOR UPDATE, garantizando a lo sumo un token vigente por usuario. Devuelve NULL si la cuenta no existe (respuesta indistinguible).';
