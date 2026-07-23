package uteq.edu.ec.artisync.entity.comunicacion;

import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plantilla de briefing configurable por el Creador.
 * RF-16: El creador puede crear/editar/eliminar plantillas sin afectar formularios ya enviados.
 */
@Entity
@Table(name = "briefing_plantillas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BriefingPlantilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_briefing_plantilla")
    private Long idBriefingPlantilla;

    @NotNull(message = "El perfil creador es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil", nullable = false)
    private PerfilCreador perfilCreador;

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 150, message = "El nombre de la plantilla no puede superar los 150 caracteres")
    @Column(name = "nombre_plantilla", nullable = false, length = 150)
    private String nombrePlantilla;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Builder.Default
    @OneToMany(mappedBy = "plantilla", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("numeroOrden ASC")
    private List<BriefingPregunta> preguntas = new ArrayList<>();
}
