package uteq.edu.ec.artisync.model.perfil;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "portafolios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portafolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_portafolio")
    private Long idPortafolio;

    @NotNull(message = "El perfil del creador es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil", nullable = false, unique = true)
    private PerfilCreador perfil;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Builder.Default
    @Column(name = "total_visitas_acumuladas", nullable = false)
    private Integer totalVisitasAcumuladas = 0;

    @Builder.Default
    @Column(name = "es_publico", nullable = false)
    private Boolean esPublico = true;

    @Builder.Default
    @Size(max = 20, message = "El color de plantilla no puede superar los 20 caracteres")
    @Column(name = "color_plantilla", length = 20)
    private String colorPlantilla = "#FFFFFF";
}
