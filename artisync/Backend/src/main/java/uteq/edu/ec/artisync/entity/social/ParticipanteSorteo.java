package uteq.edu.ec.artisync.entity.social;

import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "participantes_sorteo", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_sorteo", "id_usuario"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipanteSorteo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participacion")
    private Long idParticipacion;

    @NotNull(message = "El sorteo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sorteo", nullable = false)
    private Sorteo sorteo;

    @NotNull(message = "El usuario participante es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @CreationTimestamp
    @Column(name = "fecha_inscripcion", updatable = false)
    private LocalDateTime fechaInscripcion;

    @Builder.Default
    @Column(name = "es_ganador", nullable = false)
    private Boolean esGanador = false;

    @Column(name = "fecha_notificacion_premio")
    private LocalDateTime fechaNotificacionPremio;
}
