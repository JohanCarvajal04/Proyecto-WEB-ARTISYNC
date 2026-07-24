package uteq.edu.ec.artisync.dto.peticion.comunicacion;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO para que el Creador envíe un briefing a un pedido.
 * El id de pedido va en el path variable; aquí se indica qué plantilla usar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionEnviarBriefing {

    @NotNull(message = "El id de la plantilla es obligatorio")
    private Long idBriefingPlantilla;
}
