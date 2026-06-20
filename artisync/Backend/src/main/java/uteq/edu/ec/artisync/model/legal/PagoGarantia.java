package uteq.edu.ec.artisync.model.legal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

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
}
