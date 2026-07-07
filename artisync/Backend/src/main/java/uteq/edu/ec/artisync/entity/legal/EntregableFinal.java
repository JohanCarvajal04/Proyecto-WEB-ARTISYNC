package uteq.edu.ec.artisync.entity.legal;

import uteq.edu.ec.artisync.entity.pedido.Pedido;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "entregables_finales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregableFinal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entregable")
    private Long idEntregable;

    @NotNull(message = "El pedido es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Size(max = 255, message = "La URL de la version con marca de agua no puede superar los 255 caracteres")
    @Column(name = "url_version_marca_agua", length = 255)
    private String urlVersionMarcaAgua;

    @Size(max = 255, message = "La URL de la version limpia no puede superar los 255 caracteres")
    @Column(name = "url_version_limpia", length = 255)
    private String urlVersionLimpia;

    @Builder.Default
    @Column(name = "esta_liberado", nullable = false)
    private Boolean estaLiberado = false;
}
