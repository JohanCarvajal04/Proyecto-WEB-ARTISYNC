package uteq.edu.ec.artisync.dto.peticion.comunicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para enviar un mensaje de chat.
 * El id de sala se obtiene del path variable y el remitente del JWT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionEnviarMensaje {

    @NotBlank(message = "El cuerpo del mensaje no puede estar vacío")
    @Size(max = 5000, message = "El mensaje no puede superar los 5000 caracteres")
    private String cuerpoMensaje;
}
