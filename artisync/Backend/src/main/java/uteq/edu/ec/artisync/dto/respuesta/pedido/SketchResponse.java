package uteq.edu.ec.artisync.dto.respuesta.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SketchResponse {

    private Long idBoceto;
    private Long idPedido;
    private String urlImagen;
    private LocalDateTime fechaSubida;
}
