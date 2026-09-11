package uteq.edu.ec.artisync.dto.respuesta.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de los detalles de una solicitud de correccion sobre un entregable.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisionTicketResponse {

    private Long idTicket;
    private Long idPedido;
    private String descripcionMotivo;
    private String descripcionCliente;
    private String estadoTicket;
    private BigDecimal costoAdicionalGenerado;
    private String urlPagoAdicional;
}



