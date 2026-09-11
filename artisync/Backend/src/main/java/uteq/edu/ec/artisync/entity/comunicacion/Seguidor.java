package uteq.edu.ec.artisync.entity.comunicacion;

import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seguidores", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_usuario_seguidor", "id_perfil_creador"})
})
/**
 * Entidad del modelo de dominio que representa Relacion unilateral donde un usuario se suscribe a las actualizaciones de un creador.
 * 
 * Ciclo de vida: Ciclo de vida volatil, dependiente de la intencion del usuario de mantener la suscripcion.
 * 
 * Relaciones principales: Relacion reflexiva indirecta (Usuario origen a Usuario creador).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seguidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seguimiento")
    private Long idSeguimiento;

    @NotNull(message = "El usuario seguidor es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_seguidor", nullable = false)
    private Usuario usuarioSeguidor;

    @NotNull(message = "El perfil del creador es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil_creador", nullable = false)
    private PerfilCreador perfilCreador;

    @CreationTimestamp
    @Column(name = "fecha_seguimiento", updatable = false)
    private LocalDateTime fechaSeguimiento;

    @Builder.Default
    @Column(name = "notificaciones_activas", nullable = false)
    private Boolean notificacionesActivas = true;
}


