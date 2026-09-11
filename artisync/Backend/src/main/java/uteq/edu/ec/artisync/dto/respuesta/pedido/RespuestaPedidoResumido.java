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
 * Propósito: Proyeccion ligera de un pedido, optimizada para la bandeja de entrada o dashboard.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaPedidoResumido {

    private Long idPedido;
    private String tituloServicio;
    private String etapaActual;
    private BigDecimal precioPactado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaEntregaEstimada;
    private String nombreCreador;
    private String nombreCliente;
}



