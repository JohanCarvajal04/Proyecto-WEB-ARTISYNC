package uteq.edu.ec.artisync.service.legal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.legal.WithdrawalRequestFilter;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateWithdrawalRequest;
import uteq.edu.ec.artisync.dto.respuesta.legal.CreatorBalanceResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.WithdrawalRequestResponse;

import java.util.List;

public interface IWithdrawalRequestService {

    /**
     * Calcula el saldo disponible para retiro de un creador y si ya tiene
     * el correo de PayPal configurado o una solicitud en curso.
     *
     * @param idUsuarioCreador id del usuario creador
     * @return el saldo disponible y el estado de configuración/solicitud del creador
     */
    CreatorBalanceResponse getBalance(Long idUsuarioCreador);

    /**
     * Crea una solicitud de retiro para un creador, contra su saldo disponible.
     *
     * @param idUsuarioCreador id del usuario creador que solicita el retiro
     * @param peticion         monto solicitado
     * @return la solicitud recién creada, en estado "Pendiente"
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el creador no ha configurado su correo de PayPal, ya tiene una solicitud en curso, el monto es menor al mínimo permitido, o supera su saldo disponible
     */
    WithdrawalRequestResponse request(Long idUsuarioCreador, CreateWithdrawalRequest peticion);

    /**
     * Lista las solicitudes de retiro de un creador.
     *
     * @param idUsuarioCreador id del usuario creador
     * @return las solicitudes del creador
     */
    List<WithdrawalRequestResponse> myRequests(Long idUsuarioCreador);

    /**
     * Lista, paginadas, las solicitudes de retiro que cumplen el filtro indicado (cola de administración).
     *
     * @param filtro   criterios de filtrado (estado, rango de fechas, etc.)
     * @param pageable configuración de paginación y orden
     * @return la página de solicitudes que cumplen el filtro
     */
    Page<WithdrawalRequestResponse> listQueue(WithdrawalRequestFilter filtro, Pageable pageable);

    /**
     * Ejecuta el payout real a PayPal (POST /v1/payments/payouts). Según la
     * respuesta, la solicitud queda en "Pagado" (SUCCESS), sigue "Aprobado"
     * (PENDING/UNCLAIMED, PayPal la resuelve más tarde) o pasa a "Fallido"
     * (error/DENIED), con {@code mensajeError} poblado en ese último caso.
     *
     * @param idSolicitud id de la solicitud a approve, debe estar en estado "Pendiente"
     * @param idAdmin     id del administrador que aprueba
     * @return la solicitud con su estado final ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la solicitud o el administrador no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la solicitud no está en estado "Pendiente"
     */
    WithdrawalRequestResponse approve(Long idSolicitud, Long idAdmin);

    /**
     * Rechaza una solicitud de retiro pendiente, con un motivo obligatorio.
     *
     * @param idSolicitud id de la solicitud a reject, debe estar en estado "Pendiente"
     * @param idAdmin     id del administrador que rechaza
     * @param notaAdmin   motivo del rechazo, obligatorio
     * @return la solicitud ya marcada como rechazada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la solicitud o el administrador no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si no se indica un motivo, o si la solicitud no está en estado "Pendiente"
     */
    WithdrawalRequestResponse reject(Long idSolicitud, Long idAdmin, String notaAdmin);

    /**
     * Solo permitido desde "Fallido". Reintenta el mismo payout reusando el
     * sender_batch_id original ("retiro-" + idSolicitud): si PayPal ya había
     * procesado el intento anterior pese al error de red/respuesta, el batch
     * id idéntico hace que PayPal deduplique en vez de cobrar dos veces.
     *
     * @param idSolicitud id de la solicitud a retry, debe estar en estado "Fallido"
     * @param idAdmin     id del administrador que reintenta
     * @return la solicitud con su estado final ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la solicitud o el administrador no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la solicitud no está en estado "Fallido"
     */
    WithdrawalRequestResponse retry(Long idSolicitud, Long idAdmin);
}
