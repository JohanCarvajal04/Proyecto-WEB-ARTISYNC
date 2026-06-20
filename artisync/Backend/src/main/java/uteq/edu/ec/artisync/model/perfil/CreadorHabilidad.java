package uteq.edu.ec.artisync.model.perfil;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "creador_habilidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreadorHabilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_creador_habilidad")
    private Long idCreadorHabilidad;

    @NotNull(message = "El perfil del creador es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil", nullable = false)
    private PerfilCreador perfil;

    @NotNull(message = "La habilidad es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_habilidad", nullable = false)
    private Habilidad habilidad;

    @Size(max = 50, message = "El nivel de dominio no puede superar los 50 caracteres")
    @Column(name = "nivel_dominio", length = 50)
    private String nivelDominio;
}
