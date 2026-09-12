package uteq.edu.ec.artisync.entity.pedido;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad del modelo de dominio que representa el boceto/avance con marca de
 * agua que el creador comparte con el cliente antes de la entrega final.
 *
 * Ciclo de vida: una fila por pedido, que se resube y reemplaza (mismo patron
 * que FinalDeliverable). No lleva historial de versiones anteriores.
 *
 * Relaciones principales: pertenece a un Order.
 */
@Entity
@Table(name = "bocetos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sketch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_boceto")
    private Long idBoceto;

    @NotNull(message = "El pedido es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Order pedido;

    @NotBlank(message = "La URL de la imagen es obligatoria")
    @Size(max = 255, message = "La URL de la imagen no puede superar los 255 caracteres")
    @Column(name = "url_imagen", length = 255, nullable = false)
    private String urlImagen;

    @UpdateTimestamp
    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;
}
