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
    private List<RespuestaSubcategoria> subcategorias;
    private Long idPerfilCreador;
    private String nombreCreador;
    private Long idFlujo;
    private String nombreFlujo;
    private Long idPlantillaContrato;
    private String nombrePlantillaContrato;
    private Long idBriefingPlantilla;
    private String nombreBriefingPlantilla;
    /** Preguntas del cuestionario asignado, para responderlas al crear el pedido. Vacía/null si no tiene uno. */
    private List<PreguntaBriefingItem> preguntasBriefing;
    private List<RespuestaAtributo> atributos;
    private List<RespuestaEtiqueta> etiquetas;
    private LocalDateTime actualizadoEn;

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreguntaBriefingItem {
        private Long idPregunta;
        private String textoPregunta;
        private Integer numeroOrden;
    }
}
