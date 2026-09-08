package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.TokenResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;

public interface AuthService {
    /**
     * Registra una nueva cuenta de usuario, validando correo único, mayoría de edad y rol permitido.
     *
     * @param request datos de registro (nombres, correo, contraseña, fecha de nacimiento, rol)
     * @return el usuario recién registrado
     */
    UserResponse register(RegisterRequest request);

    /**
     * Autentica a un usuario con correo y contraseña. Si tiene 2FA habilitado devuelve un ticket
     * de preautenticación en vez de tokens de sesión; si no, emite access y refresh token de una vez.
     *
     * @param request correo y contraseña
     * @return los tokens de sesión, o un ticket de preautenticación si requiere 2FA
     */
    TokenResponse login(LoginRequest request);

    /**
     * Completa el login en dos pasos verificando el código de 2FA (o de respaldo) contra el ticket
     * emitido por {@link #login}, y emite los tokens de sesión definitivos.
     *
     * @param preAuthTicket ticket de un solo uso emitido tras validar la contraseña
     * @param request       código de 2FA o de respaldo ingresado por el usuario
     * @return los tokens de sesión ya emitidos
     */
    TokenResponse verify2Fa(String preAuthTicket, TwoFactorRequest request);

    /**
     * Renueva el par de tokens de sesión a partir de un refresh token válido y no revocado.
     *
     * @param refreshToken refresh token vigente
     * @return el nuevo par de access y refresh token
     */
    TokenResponse refreshToken(String refreshToken);

    /**
     * Cierra la sesión actual, revocando el access token y, si se envía, el refresh token asociado.
     *
     * @param tokenHeader  cabecera Authorization con el access token
     * @param refreshToken refresh token a revocar, o {@code null}
     * @return mensaje de confirmación
     */
    RespuestaMensaje logout(String tokenHeader, String refreshToken);

    /**
     * Inicia el flujo de recuperación de contraseña enviando un correo con un token de un solo uso,
     * si la cuenta existe. La respuesta es siempre la misma exista o no la cuenta, para no filtrar
     * qué correos están registrados.
     *
     * @param request correo de la cuenta a recuperar
     * @return mensaje de confirmación, indistinguible entre cuenta existente o no
     */
    RespuestaMensaje forgotPassword(ForgotPasswordRequest request);

    /**
     * Restablece la contraseña de una cuenta a partir de un token de recuperación válido y no expirado.
     *
     * @param request token de recuperación y nueva contraseña
     * @return mensaje de confirmación
     */
    RespuestaMensaje resetPassword(ResetPasswordRequest request);
}

