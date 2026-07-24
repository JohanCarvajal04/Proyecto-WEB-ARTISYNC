package uteq.edu.ec.artisync.dto.respuesta.comunicacion;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un mensaje de chat.
 * Incluye id de sala para que el frontend pueda enrutar a /topic/sala.{idSala}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaMensaje {

    private Long idMensaje;
    private Long idSala;
    private Long idRemitente;
    private String nombreRemitente;
    private String cuerpoMensaje;
    private LocalDateTime fechaHoraEnvio;
    private Boolean leido;
}
