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
public class RespuestaHistorialEstado {

    private Long idHistorial;
    private String nombreEtapa;
    private LocalDateTime fechaTransicion;
    private String observacion;
}
