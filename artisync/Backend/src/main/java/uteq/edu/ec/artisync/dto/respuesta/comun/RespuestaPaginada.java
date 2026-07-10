package uteq.edu.ec.artisync.dto.respuesta.comun;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaPaginada<T> {
    private List<T> contenido;
    @JsonProperty("numeroPagina")
    private int numeroPagina;
    @JsonProperty("tamanoPagina")
    private int tamanoPagina;
    @JsonProperty("totalElementos")
    private long totalElementos;
    @JsonProperty("totalPaginas")
    private int totalPaginas;
    @JsonProperty("esUltima")
    private boolean esUltima;
}
