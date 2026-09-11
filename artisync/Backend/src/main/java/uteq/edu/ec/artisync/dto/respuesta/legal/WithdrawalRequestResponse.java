package uteq.edu.ec.artisync.dto.respuesta.legal;

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



