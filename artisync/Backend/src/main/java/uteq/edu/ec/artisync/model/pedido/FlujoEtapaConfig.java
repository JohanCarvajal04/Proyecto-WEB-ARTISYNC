package uteq.edu.ec.artisync.model.pedido;

import uteq.edu.ec.artisync.model.catalogo.FlujoTrabajo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "flujo_etapas_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlujoEtapaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_flujo_etapa")
    private Long idFlujoEtapa;

    @NotNull(message = "El flujo de trabajo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_flujo", nullable = false)
    private FlujoTrabajo flujo;

    @NotNull(message = "La etapa es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_etapa", nullable = false)
    private EtapaFlujo etapa;

    @NotNull(message = "El numero de orden es obligatorio")
    @Column(name = "numero_orden", nullable = false)
    private Integer numeroOrden;

    @Builder.Default
    @Column(name = "es_etapa_final", nullable = false)
    private Boolean esEtapaFinal = false;
}
