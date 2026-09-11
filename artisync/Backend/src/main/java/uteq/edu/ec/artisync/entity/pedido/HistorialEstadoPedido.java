package uteq.edu.ec.artisync.entity.pedido;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad del modelo de dominio que representa Trazabilidad inmutable de los cambios de estado (creado, pagado, en progreso).
 * 
 * Ciclo de vida: Registro apendice inmutable. Solo permite inserciones para garantizar auditoria.
 * 
 * Relaciones principales: Ligado fuertemente a un Pedido especifico.
 */
@Entity
@Table(name = "historial_estados_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialEstadoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial_estado")
    private Long idHistorialEstado;

    @NotNull(message = "El pedido es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @NotNull(message = "La etapa es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_etapa", nullable = false)
    private EtapaFlujo etapa;

    @CreationTimestamp
    @Column(name = "fecha_transicion", updatable = false)
    private LocalDateTime fechaTransicion;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;
}


