package uteq.edu.ec.artisync.entity.comunicacion;

import uteq.edu.ec.artisync.entity.perfil.PortafolioItem;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comentarios_portafolio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComentarioPortafolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comentario")
    private Long idComentario;

    @NotNull(message = "El item de portafolio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_item_portafolio", nullable = false)
    private PortafolioItem itemPortafolio;

    @NotNull(message = "El autor del comentario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_autor", nullable = false)
    private Usuario usuarioAutor;

    @NotBlank(message = "El texto del comentario es obligatorio")
    @Column(name = "texto_comentario", nullable = false, columnDefinition = "TEXT")
    private String textoComentario;

    @CreationTimestamp
    @Column(name = "fecha_publicacion", updatable = false)
    private LocalDateTime fechaPublicacion;

    @Builder.Default
    @Size(max = 50, message = "El estado de moderacion no puede superar los 50 caracteres")
    @Column(name = "estado_moderacion", length = 50)
    private String estadoModeracion = "Activo";
}
