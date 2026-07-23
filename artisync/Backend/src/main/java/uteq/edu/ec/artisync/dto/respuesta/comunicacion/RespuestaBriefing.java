package uteq.edu.ec.artisync.dto.respuesta.comunicacion;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para un briefing (plantilla + estado de envío + respuestas).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaBriefing {

    private Long idBriefingEnviado;
    private Long idPedido;
    private Long idPlantilla;
    private String nombrePlantilla;
    private LocalDateTime fechaEnvio;
    private Boolean completado;
    private List<PreguntaRespuestaItem> preguntas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreguntaRespuestaItem {
        private Long idPregunta;
        private String textoPregunta;
        private Integer numeroOrden;
        private String textoRespuesta;   // null si aún no fue respondida
        private LocalDateTime fechaRespuesta;
    }
}
