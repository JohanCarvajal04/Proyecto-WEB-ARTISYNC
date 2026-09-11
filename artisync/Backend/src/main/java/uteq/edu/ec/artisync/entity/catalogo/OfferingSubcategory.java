package uteq.edu.ec.artisync.entity.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Entidad del modelo de dominio que representa Entidad asociativa para clasificar un servicio dentro de subcategorias predefinidas.
 * 
 * Ciclo de vida: Gestionada por JPA. Ciclo de vida atado al servicio para facilitar filtros de busqueda en el catalogo.
 * 
 * Relaciones principales: Relaciona la jerarquia de categorias maestras con los servicios publicos.
 */
@Entity
@Table(name = "servicio_subcategorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferingSubcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio_subcategoria")
    private Long idServicioSubcategoria;

    @NotNull(message = "El servicio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Offering servicio;

    @NotNull(message = "La subcategoria es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_subcategoria", nullable = false)
    private Subcategory subcategoria;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "actualizado_en")
    private java.time.LocalDateTime actualizadoEn;
}


