package uteq.edu.ec.artisync.entity.comunicacion;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Pregunta individual dentro de una plantilla de briefing.
 * RF-16: Máximo 10 preguntas por plantilla.
 */
@Entity
@Table(name = "briefing_preguntas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BriefingPregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pregunta")
    private Long idPregunta;

    @NotNull(message = "La plantilla es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_briefing_plantilla", nullable = false)
    private BriefingPlantilla plantilla;

    @NotBlank(message = "El texto de la pregunta es obligatorio")
    @Column(name = "texto_pregunta", nullable = false, columnDefinition = "TEXT")
    private String textoPregunta;

    @NotNull(message = "El número de orden es obligatorio")
    @Min(value = 1, message = "El orden mínimo es 1")
    @Max(value = 10, message = "Máximo 10 preguntas por plantilla")
    @Column(name = "numero_orden", nullable = false)
    private Integer numeroOrden;
}
