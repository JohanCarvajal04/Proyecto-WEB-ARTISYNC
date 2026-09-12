package uteq.edu.ec.artisync.service.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.ViolationResponse;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;

/**
 * Offering de gestión de infracciones y suspensiones de cuenta.
 * RF-15: 3 infracciones en 30 días → suspensión temporal de 15 días.
 */
public interface ViolationService {

    /**
     * Registra una infracción por dato de contacto detectado.
     * Si se alcanzan 3 infracciones en 30 días, suspende la cuenta automáticamente.
     *
     * @param idUsuario id del usuario que intentó enviar el dato de contacto
     * @param idPedido  id del pedido relacionado al chat donde ocurrió
     * @param mensaje   texto del mensaje que contenía el dato de contacto
     */
    void registerViolation(Long idUsuario, Long idPedido, String mensaje);

    /** Lista todas las infracciones (solo ADMIN). */
    Page<ViolationResponse> listViolations(Pageable pageable);

    /** Historial de infracciones de un usuario específico (solo ADMIN). */
    Page<ViolationResponse> getHistoryByUser(Long idUsuario, Pageable pageable);

    /** Revierte una suspensión reactivando la cuenta del usuario (solo ADMIN). */
    RespuestaMensaje revertSuspension(Long idUsuario);
}
