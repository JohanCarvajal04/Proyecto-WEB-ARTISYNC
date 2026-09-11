package uteq.edu.ec.artisync.entity.comunicacion;

import uteq.edu.ec.artisync.entity.perfil.PortfolioItem;
import uteq.edu.ec.artisync.entity.seguridad.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "likes_portafolio", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_item_portafolio", "id_usuario"})
})
/**
 * Entidad del modelo de dominio que representa Interaccion positiva ('Me gusta') sobre obras del portafolio.
 * 
 * Ciclo de vida: Registro volatil (creacion/eliminacion directa) que altera los contadores de popularidad.
 * 
 * Relaciones principales: Entidad asociativa entre User y PortfolioItem.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_like")
    private Long idLike;

    @NotNull(message = "El item de portafolio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_item_portafolio", nullable = false)
    private PortfolioItem itemPortafolio;

    @NotNull(message = "El usuario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    @CreationTimestamp
    @Column(name = "fecha_like", updatable = false)
    private LocalDateTime fechaLike;
}


