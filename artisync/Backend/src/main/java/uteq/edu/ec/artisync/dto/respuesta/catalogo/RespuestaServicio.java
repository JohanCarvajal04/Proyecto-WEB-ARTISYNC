package uteq.edu.ec.artisync.dto.respuesta.catalogo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaServicio {

    private Long idServicio;
    private String tituloServicio;
    private String descripcionDetallada;
    private BigDecimal precioBase;
    private String tipoItem;
    private String estadoPublicacion;
    private String urlMiniatura;
    private BigDecimal cargoRevisionAdicional;
    private Integer limiteRevisionesBase;
    private Long idSubcategoria;
    private String nombreSubcategoria;
    private Long idCategoria;
    private String nombreCategoria;
    private Long idPerfilCreador;
    private String nombreCreador;
    private List<RespuestaAtributo> atributos;
    private List<RespuestaEtiqueta> etiquetas;
    private LocalDateTime actualizadoEn;
}
