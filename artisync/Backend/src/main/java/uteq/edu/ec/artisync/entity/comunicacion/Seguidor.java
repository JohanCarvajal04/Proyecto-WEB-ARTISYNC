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
