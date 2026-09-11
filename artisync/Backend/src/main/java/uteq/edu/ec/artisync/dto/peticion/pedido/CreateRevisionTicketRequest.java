package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload enviado por el cliente para solicitar ajustes sobre un entregable.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRevisionTicketRequest {

    @NotNull(message = "El ID del motivo es obligatorio")
    private Long idMotivo;

    @NotBlank(message = "La descripcion del cliente es obligatoria")
    private String descripcionCliente;
}



