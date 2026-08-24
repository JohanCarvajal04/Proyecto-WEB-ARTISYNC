package uteq.edu.ec.artisync.dto.peticion.comunicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para enviar un mensaje de chat.
 * Por REST el id de pedido se obtiene del path variable y el remitente del JWT;
 * por WebSocket (STOMP) no hay path variable disponible, por lo que {@code idPedido}
 * debe incluirse en el body del mensaje.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionEnviarMensaje {

    /** Requerido solo para el envío vía STOMP (/app/chat.enviar); ignorado en el endpoint REST. */
    private Long idPedido;

    @NotBlank(message = "El cuerpo del mensaje no puede estar vacío")
    @Size(max = 5000, message = "El mensaje no puede superar los 5000 caracteres")
    private String cuerpoMensaje;
}
