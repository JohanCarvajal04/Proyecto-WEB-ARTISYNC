package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearTicketRevision {

    @NotNull(message = "El ID del motivo es obligatorio")
    private Long idMotivo;

    @NotBlank(message = "La descripcion del cliente es obligatoria")
    private String descripcionCliente;
}
