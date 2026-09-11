package uteq.edu.ec.artisync.entity.legal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad del modelo de dominio que representa Fondos retenidos en Escrow (garantia) hasta la liberacion del entregable.
 *
 * Ciclo de vida: Ciclo de vida estricto: INICIADO -> RETENIDO -> LIBERADO o REEMBOLSADO. Controla flujo financiero.
 *
 * Relaciones principales: Asociada a un Pedido y dependiente de pasarelas de pago externas.
 */
@Entity
@Table(name = "pagos_garantia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoGarantia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long idPago;

    @NotNull(message = "El contrato es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato", nullable = false, unique = true)
    private Contrato contrato;

    @Size(max = 100, message = "El ID de la orden de PayPal no puede superar los 100 caracteres")
    @Column(name = "id_orden_paypal", length = 100)
    private String idOrdenPaypal;

    @NotNull(message = "El monto retenido es obligatorio")
    @Column(name = "monto_retenido", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoRetenido;

    @Builder.Default
    @Size(max = 50, message = "El estado de los fondos no puede superar los 50 caracteres")
    @Column(name = "estado_fondos", length = 50)
    private String estadoFondos = "Retenido";

    /** Motivo del ultimo intento fallido de reembolso (REQ-NF-019); null si nunca fallo uno. */
    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Usada por ReconciliacionPayPalScheduler para detectar pagos 'Pendiente'
     * estancados: crearOrdenPayPal reutiliza esta misma fila en cada
     * reintento del cliente, así que lo relevante es la edad del ULTIMO
     * intento, no de la fila original.
     */
    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
