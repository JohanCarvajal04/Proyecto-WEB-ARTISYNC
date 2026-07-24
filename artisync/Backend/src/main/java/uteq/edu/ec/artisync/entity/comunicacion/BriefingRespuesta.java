package uteq.edu.ec.artisync.entity.comunicacion;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Respuesta del Cliente a una pregunta del briefing.
 * RF-16: Las respuestas son inmutables una vez enviadas. La unicidad
 * (id_briefing_enviado, id_pregunta) está garantizada a nivel de BD.
 */
@Entity
@Table(
    name = "briefing_respuestas",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_briefing_enviado", "id_pregunta"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BriefingRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta")
    private Long idRespuesta;

    @NotNull(message = "El briefing enviado es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_briefing_enviado", nullable = false)
    private BriefingEnviado briefingEnviado;

    @NotNull(message = "La pregunta es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pregunta", nullable = false)
    private BriefingPregunta pregunta;

    @NotBlank(message = "El texto de la respuesta es obligatorio")
    @Column(name = "texto_respuesta", nullable = false, columnDefinition = "TEXT")
    private String textoRespuesta;

    @CreationTimestamp
    @Column(name = "fecha_respuesta", updatable = false)
    private LocalDateTime fechaRespuesta;
}
