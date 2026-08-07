package uteq.edu.ec.artisync.entity.perfil;

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

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "opciones_personalizacion", columnDefinition = "jsonb")
    private java.util.Map<String, String> opcionesPersonalizacion;
}
