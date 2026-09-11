package uteq.edu.ec.artisync.entity.pedido;

import uteq.edu.ec.artisync.entity.seguridad.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad del modelo de dominio que representa Negociacion preliminar de plazos y costos antes de formalizar el contrato.
 * 
 * Ciclo de vida: Ciclo volatil y transaccional: se envia, se contrarresta o se acepta, tras lo cual se materializa en un Contrato.
 * 
 * Relaciones principales: Anidada temporalmente a un Pedido en etapa temprana.
 */
@Entity
@Table(name = "propuestas_terminos_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropuestaTerminosPedido {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String ACEPTADA = "ACEPTADA";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String CANCELADA = "CANCELADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_propuesta")
    private Long idPropuesta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_propuso", nullable = false)
    private User propuestoPor;

    @Column(name = "precio_propuesto", precision = 10, scale = 2)
    private BigDecimal precioPropuesto;

    @Column(name = "fecha_entrega_propuesta")
    private LocalDateTime fechaEntregaPropuesta;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = PENDIENTE;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
}


