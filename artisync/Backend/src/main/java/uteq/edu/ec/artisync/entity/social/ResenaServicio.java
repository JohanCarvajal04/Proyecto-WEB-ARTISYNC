package uteq.edu.ec.artisync.entity.social;

import uteq.edu.ec.artisync.entity.pedido.Pedido;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resenas_servicios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResenaServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Long idResena;

    @NotNull(message = "El pedido es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @Min(value = 1, message = "La calificacion minima es 1 estrella")
    @Max(value = 5, message = "La calificacion maxima es 5 estrellas")
    @Column(name = "calificacion_estrellas")
    private Integer calificacionEstrellas;

    @Column(name = "texto_resena", columnDefinition = "TEXT")
    private String textoResena;

    @CreationTimestamp
    @Column(name = "fecha_resena", updatable = false)
    private LocalDateTime fechaResena;
}
