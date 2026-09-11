package uteq.edu.ec.artisync.dto.respuesta.legal;

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
 */
@Builder
public record RespuestaSaldoCreador(
        BigDecimal saldoDisponible,
        BigDecimal montoMinimoRetiro,
        boolean tieneCorreoPaypalConfigurado,
        boolean tieneSolicitudPendiente
) {
}



