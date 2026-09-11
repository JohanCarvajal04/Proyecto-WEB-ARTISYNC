package uteq.edu.ec.artisync.entity.perfil;

import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad del modelo de dominio que representa Informacion publica, tarifas base y metricas del creador.
 * 
 * Ciclo de vida: Mantenida activamente por el usuario. Evoluciona con su reputacion y metricas acumuladas.
 * 
 * Relaciones principales: Extension 1:1 de la identidad del Usuario. Actua como puerta de entrada a sus Servicios y Portafolio.
 */
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

    @Size(max = 500, message = "La URL de la portada no puede superar los 500 caracteres")
    @Column(name = "url_portada", length = 500)
    private String urlPortada;

    @Size(max = 150, message = "El titulo profesional no puede superar los 150 caracteres")
    @Column(name = "titulo_profesional", length = 150)
    private String tituloProfesional;
}


