package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearFlujoTrabajo {

    @NotBlank(message = "El nombre del flujo de trabajo es obligatorio")
    private String nombreFlujo;

    private String descripcionFlujo;

    @Valid
    private List<PeticionEtapaConfig> etapas;
}
