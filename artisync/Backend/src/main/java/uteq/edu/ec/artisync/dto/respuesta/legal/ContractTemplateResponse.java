package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO completo para el catálogo de plantillas de contrato, uso administrativo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractTemplateResponse {

    private Long idPlantilla;
    private String nombrePlantilla;
    private String versionLegal;
    private String cuerpoHtmlPlantilla;
    private Boolean esPredeterminada;
    private Boolean activa;

    /** NULL: plantilla del catálogo general (ADMIN). No NULL: plantilla privada de ese creador (V45). */
    private Long idCreador;
}
