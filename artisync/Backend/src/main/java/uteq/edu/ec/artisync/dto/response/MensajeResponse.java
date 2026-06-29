package uteq.edu.ec.artisync.dto.response;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class MensajeResponse {
    private String mensaje;
    private boolean exito;
}
