package uteq.edu.ec.artisync.entity.catalogo;

import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "servicios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Long idServicio;

    @NotNull(message = "El perfil del creador es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil", nullable = false)
    private PerfilCreador perfil;

    @NotNull(message = "La subcategoria es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_subcategoria", nullable = false)
    private Subcategoria subcategoria;

    @NotBlank(message = "El titulo del servicio es obligatorio")
    @Size(max = 150, message = "El titulo del servicio no puede superar los 150 caracteres")
    @Column(name = "titulo_servicio", nullable = false, length = 150)
    private String tituloServicio;

    @NotBlank(message = "La descripcion detallada es obligatoria")
    @Size(min = 20, max = 2000, message = "La descripcion debe tener entre 20 y 2000 caracteres")
    @Column(name = "descripcion_detallada", nullable = false, columnDefinition = "TEXT")
    private String descripcionDetallada;

    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser de al menos 0.01 USD")
    @Column(name = "precio_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBase;

    @Size(max = 255, message = "La URL de la miniatura no puede superar los 255 caracteres")
    @Column(name = "url_miniatura", length = 255)
    private String urlMiniatura;
}
