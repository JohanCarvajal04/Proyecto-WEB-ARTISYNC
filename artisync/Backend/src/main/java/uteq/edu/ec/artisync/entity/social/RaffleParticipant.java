package uteq.edu.ec.artisync.entity.social;

import uteq.edu.ec.artisync.entity.seguridad.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Participación de un usuario en un sorteo, y si resultó ganador. */
@Entity
@Table(name = "participantes_sorteo", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_sorteo", "id_usuario"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaffleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participacion")
    private Long idParticipacion;

    @NotNull(message = "El sorteo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sorteo", nullable = false)
    private Raffle sorteo;

    @NotNull(message = "El usuario participante es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    @CreationTimestamp
    @Column(name = "fecha_inscripcion", updatable = false)
    private LocalDateTime fechaInscripcion;

    @Builder.Default
    @Column(name = "es_ganador", nullable = false)
    private Boolean esGanador = false;

    @Column(name = "fecha_notificacion_premio")
    private LocalDateTime fechaNotificacionPremio;

    /** Premio ganado. Nulo mientras no sea ganador o si el sorteo aun no se ha cerrado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_premio")
    private RafflePrize premio;
}
