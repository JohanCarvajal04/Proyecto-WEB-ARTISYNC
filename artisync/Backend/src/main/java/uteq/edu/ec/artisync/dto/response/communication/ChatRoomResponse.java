package uteq.edu.ec.artisync.dto.response.communication;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con el estado de una sala de chat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {

    private Long idSala;
    private Long idPedido;
    private Boolean salaActiva;
    private LocalDateTime fechaApertura;
}
