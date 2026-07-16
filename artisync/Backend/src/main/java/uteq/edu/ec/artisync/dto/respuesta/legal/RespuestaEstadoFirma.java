package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaEstadoFirma {

    private Long idContrato;
    private Boolean firmaCreadorCompleta;
    private Boolean firmaClienteCompleta;
    private Boolean ambasFirmasCompletas;
    private String mensajeEstado;
}
