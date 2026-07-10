package uteq.edu.ec.artisync.dto.respuesta.catalogo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaServicioResumido {

    private Long idServicio;
    private String tituloServicio;
    private BigDecimal precioBase;
    private String tipoItem;
    private String estadoPublicacion;
    private String urlMiniatura;
    private Long idSubcategoria;
    private String nombreSubcategoria;
    private Long idCategoria;
    private String nombreCategoria;
    private Long idPerfilCreador;
    private String nombreCreador;
    private List<RespuestaEtiqueta> etiquetas;
}
