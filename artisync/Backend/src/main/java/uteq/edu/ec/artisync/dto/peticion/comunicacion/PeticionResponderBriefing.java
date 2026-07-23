package uteq.edu.ec.artisync.dto.peticion.comunicacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * DTO para que el Cliente responda el briefing enviado.
 * RF-16: Una vez enviado, las respuestas son inmutables.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionResponderBriefing {

    @NotNull(message = "La lista de respuestas es obligatoria")
    @Valid
    private List<RespuestaItem> respuestas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RespuestaItem {

        @NotNull(message = "El id de la pregunta es obligatorio")
        private Long idPregunta;

        @NotBlank(message = "La respuesta no puede estar vacía")
        private String textoRespuesta;
    }
}
