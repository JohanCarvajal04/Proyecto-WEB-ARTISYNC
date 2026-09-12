package uteq.edu.ec.artisync.entity.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Subcategoría de una categoría del catálogo. Puede crearla un admin/moderador (ya revisada) o un creador (pendiente de revisión). */
@Entity
@Table(name = "subcategorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_subcategoria")
    private Long idSubcategoria;

    @NotNull(message = "La categoria es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Category categoria;

    @NotBlank(message = "El nombre de la subcategoria es obligatorio")
    @Size(max = 100, message = "El nombre de la subcategoria no puede superar los 100 caracteres")
    @Column(name = "nombre_subcategoria", nullable = false, length = 100)
    private String nombreSubcategoria;

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
