package uteq.edu.ec.artisync.entity.social;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "premios_sorteo", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id_sorteo", "orden"})
})
/**
 * Entidad del modelo de dominio que representa Recompensas gamificadas para usuarios.
 * 
 * Ciclo de vida: Dependiente del evento sorteo (ACTIVO o RECLAMADO).
 * 
 * Relaciones principales: Vincula un beneficio logico o fisico a un User ganador.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremioSorteo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_premio")
    private Long idPremio;

    @NotNull(message = "El sorteo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sorteo", nullable = false)
    private Sorteo sorteo;

    @NotBlank(message = "La descripcion del premio es obligatoria")
    @Size(max = 255, message = "La descripcion del premio no puede superar los 255 caracteres")
    @Column(name = "descripcion_premio", nullable = false, length = 255)
    private String descripcionPremio;

    @NotNull(message = "El orden del premio es obligatorio")
    @Min(value = 1, message = "El orden del premio debe ser al menos 1")
    @Column(name = "orden", nullable = false)
    private Integer orden;
}


