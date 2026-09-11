package uteq.edu.ec.artisync.entity.pedido;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad del modelo de dominio que representa Solicitud de ajuste sobre un entregable preliminar enviada por el cliente.
 *
 * Ciclo de vida: Inicia ABIERTO y finaliza CERRADO tras la correcion. Bloquea la liberacion de fondos.
 *
 * Relaciones principales: Vincula un FinalDeliverable con las exigencias del Cliente.
 */
@Entity
@Table(name = "tickets_revision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevisionTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ticket")
    private Long idTicket;

    @NotNull(message = "El pedido es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Order pedido;

    @NotNull(message = "El motivo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_motivo", nullable = false)
    private RejectionReason motivo;

    @NotBlank(message = "La descripcion del cliente es obligatoria")
    @Column(name = "descripcion_cliente", nullable = false, columnDefinition = "TEXT")
    private String descripcionCliente;

    @Builder.Default
    @DecimalMin(value = "0.00", message = "El costo adicional no puede ser negativo")
    @Column(name = "costo_adicional_generado", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoAdicionalGenerado = BigDecimal.ZERO;

    @Builder.Default
    @Size(max = 50, message = "El estado del ticket no puede superar los 50 caracteres")
    @Column(name = "estado_ticket", length = 50)
    private String estadoTicket = "Abierto";

    /** REQ-F-022c: TicketRevisionExpiracionScheduler la usa para calcular "48h desde la creacion". */
    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
}
