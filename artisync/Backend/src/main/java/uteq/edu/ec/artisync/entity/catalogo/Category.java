package uteq.edu.ec.artisync.entity.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long idCategoria;

    @NotBlank(message = "El nombre de la categoria es obligatorio")
    @Size(max = 100, message = "El nombre de la categoria no puede superar los 100 caracteres")
    @Column(name = "nombre_categoria", nullable = false, unique = true, length = 100)
    private String nombreCategoria;

    @Builder.Default
    @Column(name = "estado_activa", nullable = false)
    private Boolean estadoActiva = true;

    /**
     * Dueño de autoservicio: null = la creó un admin/moderador (ya confiable).
     * No nulo = la creó un creador; empieza sin revisar (ver {@link #revisado}).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_creador")
    private uteq.edu.ec.artisync.entity.seguridad.User creador;

    @Builder.Default
    @Column(name = "revisado", nullable = false)
    private Boolean revisado = true;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "actualizado_en")
    private java.time.LocalDateTime actualizadoEn;
}
