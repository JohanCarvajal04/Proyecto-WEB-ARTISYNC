package uteq.edu.ec.artisync.dto.respuesta.comunicacion;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para infracciones de mensajes (endpoints admin).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaInfraccion {

    private Long idInfraccion;
    private Long idUsuario;
    private String nombreUsuario;
    private String correoUsuario;
    private Long idPedido;
    private String mensajeOriginal;
    private String patronDetectado;
    private LocalDateTime fechaInfraccion;
}
