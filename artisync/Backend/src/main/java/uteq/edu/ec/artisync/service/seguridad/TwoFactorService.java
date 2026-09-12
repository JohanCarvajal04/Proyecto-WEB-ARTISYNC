package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.TwoFactorSetupResponse;

public interface TwoFactorService {

    /**
     * Inicia la configuración de 2FA: genera un secreto TOTP nuevo y 8 códigos
     * de respaldo, reemplazando cualquier configuración previa del usuario.
     * El secreto y los códigos en texto plano solo se devuelven en esta llamada.
     *
     * @param correo correo del usuario que configura 2FA
     * @return el secreto TOTP (como URI {@code otpauth://}) y los códigos de respaldo en texto plano
     * @throws org.springframework.web.server.ResponseStatusException {@code NOT_FOUND} si el usuario no existe;
     *         {@code FORBIDDEN} si el usuario tiene rol CREADOR y no tiene su identidad verificada y aprobada
     */
    TwoFactorSetupResponse setup2Fa(String correo);

    /**
     * Confirma y activa la configuración de 2FA iniciada con {@link #setup2Fa}, validando el código TOTP.
     *
     * @param correo correo del usuario
     * @param codigo código TOTP ingresado
     * @return mensaje de confirmación
     * @throws org.springframework.web.server.ResponseStatusException {@code NOT_FOUND} si el usuario no existe;
     *         {@code BAD_REQUEST} si no se inició la configuración de 2FA, o si el código es inválido o expiró
     */
    RespuestaMensaje confirm2Fa(String correo, String codigo);

    /**
     * Desactiva el 2FA del usuario y purga sus códigos de respaldo, validando el código TOTP o de respaldo.
     *
     * @param correo correo del usuario
     * @param codigo código TOTP o de respaldo
     * @return mensaje de confirmación
     * @throws org.springframework.web.server.ResponseStatusException {@code NOT_FOUND} si el usuario no existe;
     *         {@code BAD_REQUEST} si el 2FA no está configurado o no está activo;
     *         {@code UNAUTHORIZED} si el código es inválido o expiró
     */
    RespuestaMensaje disable2Fa(String correo, String codigo);

    /**
     * Valida un código contra el TOTP vigente del usuario o, si no coincide, contra sus códigos de respaldo no usados.
     *
     * @param correo          correo del usuario
     * @param codigoIngresado código TOTP o de respaldo a validar
     * @return {@code true} si el código coincide con el TOTP vigente o con un código de respaldo no usado
     */
    boolean validateCodeOrBackup(String correo, String codigoIngresado);
}

