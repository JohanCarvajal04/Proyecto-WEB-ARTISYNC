package uteq.edu.ec.artisync.dto.respuesta.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de los terminos negociados preliminarmente antes del contrato.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaPropuestaTerminos {

    private Long idPropuesta;
    private Long idPedido;
    private Long idUsuarioPropuso;
    private String nombrePropuso;
    private BigDecimal precioPropuesto;
    private LocalDateTime fechaEntregaPropuesta;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaResolucion;
}



