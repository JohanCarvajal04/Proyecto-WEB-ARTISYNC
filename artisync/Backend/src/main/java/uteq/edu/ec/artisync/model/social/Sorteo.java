package uteq.edu.ec.artisync.model.social;

import uteq.edu.ec.artisync.model.perfil.PerfilCreador;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sorteos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sorteo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sorteo")
    private Long idSorteo;

    @NotNull(message = "El perfil del creador es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil_creador", nullable = false)
    private PerfilCreador perfilCreador;

    @NotBlank(message = "El titulo del sorteo es obligatorio")
    @Size(max = 150, message = "El titulo del sorteo no puede superar los 150 caracteres")
    @Column(name = "titulo_sorteo", nullable = false, length = 150)
    private String tituloSorteo;

    @NotBlank(message = "La descripcion de los premios es obligatoria")
    @Column(name = "descripcion_premios", nullable = false, columnDefinition = "TEXT")
    private String descripcionPremios;

    @Builder.Default
    @Min(value = 1, message = "La cantidad de ganadores debe ser al menos 1")
    @Column(name = "cantidad_ganadores", nullable = false)
    private Integer cantidadGanadores = 1;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de cierre es obligatoria")
    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    @Builder.Default
    @Size(max = 50, message = "El estado del sorteo no puede superar los 50 caracteres")
    @Column(name = "estado_sorteo", length = 50)
    private String estadoSorteo = "Activo";
}
