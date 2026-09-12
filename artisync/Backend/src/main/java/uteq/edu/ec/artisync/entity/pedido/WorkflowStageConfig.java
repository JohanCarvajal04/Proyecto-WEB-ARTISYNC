package uteq.edu.ec.artisync.entity.pedido;

import uteq.edu.ec.artisync.entity.catalogo.Workflow;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Entidad del modelo de dominio que representa Configuracion especifica de duracion o prerequisitos para una etapa.
 * 
 * Ciclo de vida: Depende del Workflow maestro. Se inicializa al publicar un servicio.
 * 
 * Relaciones principales: Tabla de configuracion que une WorkflowStage con Workflow.
 */
@Entity
@Table(name = "flujo_etapas_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStageConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_flujo_etapa")
    private Long idFlujoEtapa;

    @NotNull(message = "El flujo de trabajo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_flujo", nullable = false)
    private Workflow flujo;

    @NotNull(message = "La etapa es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_etapa", nullable = false)
    private WorkflowStage etapa;

    @NotNull(message = "El numero de orden es obligatorio")
    @Column(name = "numero_orden", nullable = false)
    private Integer numeroOrden;

    @Builder.Default
    @Column(name = "es_etapa_final", nullable = false)
    private Boolean esEtapaFinal = false;

    @Builder.Default
    @Column(name = "requiere_entregable", nullable = false)
    private Boolean requiereEntregable = false;

    @Builder.Default
    @Column(name = "requiere_boceto", nullable = false)
    private Boolean requiereBoceto = false;
}


