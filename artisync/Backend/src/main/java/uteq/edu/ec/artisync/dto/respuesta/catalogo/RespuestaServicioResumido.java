package uteq.edu.ec.artisync.dto.respuesta.catalogo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaServicioResumido implements Serializable {

    private Long idServicio;
    private String tituloServicio;
    private BigDecimal precioBase;
    private String tipoItem;
    private String estadoPublicacion;
    private String urlMiniatura;
    private List<RespuestaSubcategoria> subcategorias;
    private Long idPerfilCreador;
    private String nombreCreador;
    private List<RespuestaEtiqueta> etiquetas;
}
