package uteq.edu.ec.artisync.model.pedido;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tickets_revision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ticket")
    private Long idTicket;

    @NotNull(message = "El pedido es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @NotNull(message = "El motivo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_motivo", nullable = false)
    private MotivoRechazo motivo;

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
}
