package uteq.edu.ec.artisync.dto.response.legal;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion del estado de una solicitud de retiro de fondos.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 *
 * @param idSolicitud id de la solicitud de retiro
 * @param idUsuarioCreador id del usuario creador que solicita el retiro
 * @param nombreCreador nombre completo del creador
 * @param montoSolicitado monto solicitado para el retiro
 * @param correoPaypalDestino correo de PayPal de destino del pago
 * @param estado estado actual de la solicitud
 * @param idPayoutPaypal id del payout de PayPal, una vez pagado
 * @param notaAdmin nota del administrador que decidió la solicitud
 * @param mensajeError mensaje de error, si el pago falló
 * @param fechaSolicitud fecha en que se creó la solicitud
 * @param fechaDecision fecha en que un administrador la aprobó o rechazó
 * @param fechaPago fecha en que se efectuó el pago
 */
@Builder
public record WithdrawalRequestResponse(
        Long idSolicitud,
        Long idUsuarioCreador,
        String nombreCreador,
        BigDecimal montoSolicitado,
        String correoPaypalDestino,
        String estado,
        String idPayoutPaypal,
        String notaAdmin,
        String mensajeError,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaDecision,
        LocalDateTime fechaPago
) {
}



