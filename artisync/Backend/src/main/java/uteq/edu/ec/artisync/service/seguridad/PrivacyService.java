package uteq.edu.ec.artisync.service.seguridad;

import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;

/**
 * REQ-NF-018: mecanismo real de supresión de datos personales, distinto de la
 * baja lógica de cuenta ({@code estadoCuenta=false}) que ya ofrecen
 * {@link UserService#deleteOwnAccount} y {@code AdminUserService#deleteUser}.
 *
 * <p>La estrategia es anonimización, no borrado físico: se conservan las
 * filas por integridad referencial con pedidos, contratos y la bitácora de
 * auditoría (REQ-NF-013), pero se sobrescriben irreversiblemente los campos
 * identificativos. Registros con una obligación legal/contable pendiente
 * (fondos en garantía aún retenidos) quedan excluidos de la anonimización de
 * su dato de pago, y esa excepción se declara en la respuesta.
 */
public interface PrivacyService {

    /**
     * El propio titular solicita la supresión de sus datos personales.
     * Idempotente: si ya se ejecutó antes (por el mismo usuario o por un
     * administrador), no repite la operación y lo informa en el mensaje.
     *
     * <p>Si el usuario tiene 2FA activo, exige un código TOTP o de respaldo
     * válido como verificación adicional ("step-up") antes de ejecutar una
     * acción irreversible — mismo criterio que ya exige
     * {@code TwoFactorService#disable2Fa} para desactivar el propio 2FA.
     *
     * @param idUsuario identificador del usuario autenticado
     * @param codigo    código TOTP o de respaldo; puede ser {@code null} si el usuario no tiene 2FA activo
     * @return mensaje de confirmación, incluyendo qué categorías de datos quedaron excluidas por excepción legal, si las hay
     * @throws org.springframework.web.server.ResponseStatusException {@code NOT_FOUND} si el usuario no existe;
     *         {@code BAD_REQUEST} si el usuario tiene 2FA activo y no envía código;
     *         {@code UNAUTHORIZED} si el código enviado es inválido o expiró
     */
    RespuestaMensaje requestOwnErasure(Long idUsuario, String codigo);

    /**
     * Un administrador ejecuta la supresión en nombre de un usuario. A
     * diferencia del autoservicio, esta variante rechaza la operación (en vez
     * de responder de forma idempotente) si el usuario ya la tiene ejecutada,
     * para que el administrador no la dispare sin necesidad.
     *
     * @param idUsuario     identificador del usuario cuyos datos se suprimen
     * @param idAdminActual identificador del administrador que ejecuta la acción
     * @return mensaje de confirmación, incluyendo qué categorías de datos quedaron excluidas por excepción legal, si las hay
     * @throws org.springframework.web.server.ResponseStatusException {@code NOT_FOUND} si el usuario no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el usuario ya tiene la supresión ejecutada
     */
    RespuestaMensaje anonymizeUserAsAdmin(Long idUsuario, Long idAdminActual);
}
