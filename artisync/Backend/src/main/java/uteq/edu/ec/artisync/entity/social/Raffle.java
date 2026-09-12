package uteq.edu.ec.artisync.entity.social;

import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Sorteo creado por un perfil de creador, con sus premios y ganadores. */
@Entity
@Table(name = "sorteos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Raffle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sorteo")
    private Long idSorteo;

    @NotNull(message = "El perfil del creador es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil_creador", nullable = false)
    private CreatorProfile perfilCreador;

    @NotBlank(message = "El titulo del sorteo es obligatorio")
    @Size(max = 150, message = "El titulo del sorteo no puede superar los 150 caracteres")
    @Column(name = "titulo_sorteo", nullable = false, length = 150)
    private String tituloSorteo;

    @Builder.Default
    @Min(value = 1, message = "La cantidad de ganadores debe ser al menos 1")
    @Column(name = "cantidad_ganadores", nullable = false)
    private Integer cantidadGanadores = 1;

    /** Premios individuales del sorteo, uno por ganador. REQ-F-023 (V39). */
    @Builder.Default
    @OneToMany(mappedBy = "sorteo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<RafflePrize> premios = new ArrayList<>();

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

    @Builder.Default
    @Column(name = "requiere_seguidor", nullable = false)
    private Boolean requiereSeguidor = false;
}
