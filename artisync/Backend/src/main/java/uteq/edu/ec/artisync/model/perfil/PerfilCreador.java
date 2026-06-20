package uteq.edu.ec.artisync.model.perfil;

import uteq.edu.ec.artisync.model.seguridad.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "perfiles_creadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilCreador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil")
    private Long idPerfil;

    @NotNull(message = "El usuario es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Size(max = 500, message = "La biografia no puede superar los 500 caracteres")
    @Column(name = "biografia", columnDefinition = "TEXT")
    private String biografia;

    @Size(max = 255, message = "La URL de la red social no puede superar los 255 caracteres")
    @Column(name = "url_red_social", length = 255)
    private String urlRedSocial;
}
