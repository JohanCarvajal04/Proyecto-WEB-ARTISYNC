package uteq.edu.ec.artisync.entity.legal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion")
    private Long idTransaccion;

    @NotNull(message = "El pago de garantia es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago", nullable = false)
    private PagoGarantia pago;

    @NotBlank(message = "El tipo de transaccion es obligatorio")
    @Size(max = 50, message = "El tipo de transaccion no puede superar los 50 caracteres")
    @Column(name = "tipo_transaccion", nullable = false, length = 50)
    private String tipoTransaccion;

    @NotNull(message = "El monto es obligatorio")
    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @CreationTimestamp
    @Column(name = "fecha_ejecucion", updatable = false)
    private LocalDateTime fechaEjecucion;
}
