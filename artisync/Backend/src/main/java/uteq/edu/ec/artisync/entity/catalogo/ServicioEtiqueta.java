package uteq.edu.ec.artisync.entity.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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
