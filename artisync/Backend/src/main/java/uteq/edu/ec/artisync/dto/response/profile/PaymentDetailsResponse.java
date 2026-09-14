package uteq.edu.ec.artisync.dto.response.profile;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de la configuracion de cobro del creador (enmascarando datos sensibles).
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 *
 * @param correoPaypal correo de PayPal configurado para recibir pagos
 * @param fechaActualizacion fecha de la última actualización de la configuración
 */
@Builder
public record PaymentDetailsResponse(
        String correoPaypal,
        LocalDateTime fechaActualizacion
) {
}



