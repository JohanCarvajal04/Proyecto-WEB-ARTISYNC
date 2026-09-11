package uteq.edu.ec.artisync.entity.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Entidad del modelo de dominio que representa Entidad asociativa pura entre Servicio y Etiqueta.
 * 
 * Ciclo de vida: Gestionada por JPA. Se crea y destruye en conjunto con la actualizacion de metadatos del servicio.
 * 
 * Relaciones principales: Tabla puente que consolida la busqueda cruzada de servicios por tag.
 */
@Entity
@Table(name = "servicio_etiquetas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioEtiqueta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio_etiqueta")
    private Long idServicioEtiqueta;

    @NotNull(message = "El servicio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    @NotNull(message = "La etiqueta es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_etiqueta", nullable = false)
    private Etiqueta etiqueta;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "actualizado_en")
    private java.time.LocalDateTime actualizadoEn;
}


