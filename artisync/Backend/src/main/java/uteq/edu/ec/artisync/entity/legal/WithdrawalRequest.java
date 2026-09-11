package uteq.edu.ec.artisync.entity.legal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import uteq.edu.ec.artisync.entity.seguridad.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Solicitud de un creador para retirar sus fondos ya liberados (REQ-F-024). El
 * dinero real se transfiere vía PayPal Payouts al aprobar
 * (WithdrawalRequestServiceImpl); esta fila es el registro de todo el ciclo,
 * desde que se pide hasta que PayPal confirma el pago.
 */
@Entity
@Table(name = "solicitudes_retiro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    @NotNull(message = "El creador es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_creador", nullable = false)
    private User usuarioCreador;

    @NotNull(message = "El monto solicitado es obligatorio")
    @Positive(message = "El monto solicitado debe ser mayor que cero")
    @Column(name = "monto_solicitado", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoSolicitado;

    @NotBlank(message = "El correo de PayPal destino es obligatorio")
    @Size(max = 150, message = "El correo de PayPal destino no puede superar los 150 caracteres")
    @Column(name = "correo_paypal_destino", nullable = false, length = 150)
    private String correoPaypalDestino;

    @Builder.Default
    @Size(max = 20, message = "El estado no puede superar los 20 caracteres")
    @Column(name = "estado", length = 20)
    private String estado = "Pendiente";

    @Size(max = 100, message = "El id del payout de PayPal no puede superar los 100 caracteres")
    @Column(name = "id_payout_paypal", length = 100)
    private String idPayoutPaypal;

    @Size(max = 100, message = "El id del item de payout de PayPal no puede superar los 100 caracteres")
    @Column(name = "id_item_payout_paypal", length = 100)
    private String idItemPayoutPaypal;

    @Column(name = "nota_admin", columnDefinition = "TEXT")
    private String notaAdmin;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @CreationTimestamp
    @Column(name = "fecha_solicitud", updatable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin_decisor")
    private User adminDecisor;
}
