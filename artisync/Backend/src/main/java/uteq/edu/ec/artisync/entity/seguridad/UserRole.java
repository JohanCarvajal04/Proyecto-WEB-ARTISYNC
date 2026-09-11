package uteq.edu.ec.artisync.entity.seguridad;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Entidad del modelo de dominio que representa Entidad asociativa que otorga roles a un usuario.
 * 
 * Ciclo de vida: Ciclo ligado a la administracion de identidad. Actualizada ante upgrades (ej. cliente -> creador).
 * 
 * Relaciones principales: Tabla puente pura entre User y Role.
 */
@Entity
@Table(name = "usuario_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_rol")
    private Long idUsuarioRol;

    @NotNull(message = "El usuario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    @NotNull(message = "El rol es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", nullable = false)
    private Role rol;
}


