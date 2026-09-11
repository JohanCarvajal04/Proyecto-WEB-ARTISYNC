package uteq.edu.ec.artisync.entity.legal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uteq.edu.ec.artisync.entity.pedido.RevisionTicket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pago del cargo adicional de un ticket de revisión que superó el límite del
 * contrato (REQ-F-022b/c). Entidad propia, no una fila más de EscrowPayment:
 * EscrowPayment.contrato es @OneToOne con FK UNIQUE (un solo pago de garantía
 * por contrato, para siempre), y este cobro es independiente del escrow
 * principal ya verificado (REQ-F-020 / REQ-NF-014 / REQ-NF-019).
 */
@Entity
@Table(name = "pagos_ticket_revision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevisionTicketPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_ticket")
    private Long idPagoTicket;

    @NotNull(message = "El ticket de revision es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ticket", nullable = false, unique = true)
    private RevisionTicket ticket;

    @Size(max = 100, message = "El ID de la orden de PayPal no puede superar los 100 caracteres")
    @Column(name = "id_orden_paypal", length = 100)
    private String idOrdenPaypal;

    @Size(max = 255, message = "La URL de aprobación no puede superar los 255 caracteres")
    @Column(name = "url_aprobacion")
    private String urlAprobacion;

    @NotNull(message = "El monto es obligatorio")
    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Builder.Default
    @Size(max = 50, message = "El estado del pago no puede superar los 50 caracteres")
    @Column(name = "estado_pago", length = 50)
    private String estadoPago = "Pendiente";

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
