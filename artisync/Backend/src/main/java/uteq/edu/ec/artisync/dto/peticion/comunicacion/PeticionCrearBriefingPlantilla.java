package uteq.edu.ec.artisync.dto.peticion.comunicacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * DTO para crear o editar una plantilla de briefing.
 * RF-16: Máximo 10 preguntas por plantilla.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearBriefingPlantilla {

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombrePlantilla;

    @NotNull(message = "La lista de preguntas es obligatoria")
    @Size(min = 1, max = 10, message = "La plantilla debe tener entre 1 y 10 preguntas")
    @Valid
    private List<PreguntaRequest> preguntas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreguntaRequest {

        @NotBlank(message = "El texto de la pregunta es obligatorio")
        private String textoPregunta;

        @NotNull(message = "El número de orden es obligatorio")
        @Min(value = 1, message = "El orden mínimo es 1")
        @Max(value = 10, message = "El orden máximo es 10")
        private Integer numeroOrden;
    }
}
