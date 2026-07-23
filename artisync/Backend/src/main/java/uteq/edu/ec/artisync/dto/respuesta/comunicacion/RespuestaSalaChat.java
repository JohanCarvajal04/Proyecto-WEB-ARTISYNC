package uteq.edu.ec.artisync.dto.respuesta.comunicacion;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con el estado de una sala de chat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaSalaChat {

    private Long idSala;
    private Long idPedido;
    private Boolean salaActiva;
    private LocalDateTime fechaApertura;
}
