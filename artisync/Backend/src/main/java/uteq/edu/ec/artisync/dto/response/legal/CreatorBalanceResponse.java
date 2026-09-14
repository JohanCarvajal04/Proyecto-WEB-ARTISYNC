package uteq.edu.ec.artisync.dto.response.legal;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Consolidado de las metricas financieras del creador (saldo disponible, saldo retenido).
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 *
 * @param saldoDisponible saldo actualmente disponible para retiro
 * @param montoMinimoRetiro monto mínimo permitido por solicitud de retiro
 * @param tieneCorreoPaypalConfigurado si el creador ya configuró su correo de PayPal
 * @param tieneSolicitudPendiente si ya existe una solicitud de retiro pendiente
 */
@Builder
public record CreatorBalanceResponse(
        BigDecimal saldoDisponible,
        BigDecimal montoMinimoRetiro,
        boolean tieneCorreoPaypalConfigurado,
        boolean tieneSolicitudPendiente
) {
}



