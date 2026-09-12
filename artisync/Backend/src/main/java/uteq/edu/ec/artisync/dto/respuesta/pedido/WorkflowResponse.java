package uteq.edu.ec.artisync.dto.respuesta.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Respuesta con un flujo de trabajo y sus etapas configuradas. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {

    private Long idFlujo;
    private String nombreFlujo;
    private String descripcionFlujo;
    private List<StageConfigResponse> etapas;
    /** Dueño del flujo. Relevante cuando quien lista tiene FLUJO_MODERAR y ve flujos de varios creadores. */
    private Long idUsuarioCreador;
    private String nombreCreador;
}
