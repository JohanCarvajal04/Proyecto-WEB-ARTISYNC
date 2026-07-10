package uteq.edu.ec.artisync.dto.peticion.catalogo;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionActualizarServicio {

    @NotBlank(message = "El titulo del servicio es obligatorio")
    @Size(max = 150, message = "El titulo del servicio no puede superar los 150 caracteres")
    private String tituloServicio;

    @NotBlank(message = "La descripcion detallada es obligatoria")
    @Size(min = 20, max = 2000, message = "La descripcion detallada debe tener entre 20 y 2000 caracteres")
    private String descripcionDetallada;

    @NotNull(message = "El precio base es un campo obligatorio")
    @DecimalMin(value = "0.01", message = "El precio es un campo obligatorio y debe ser al menos 0.01 USD")
    private BigDecimal precioBase;

    @NotNull(message = "El ID de la subcategoria es obligatorio")
    private Long idSubcategoria;

    @NotBlank(message = "El tipo de item es obligatorio")
    @Pattern(regexp = "PRODUCTO|SERVICIO", message = "El tipo de item debe ser PRODUCTO o SERVICIO")
    private String tipoItem;

    @NotBlank(message = "El estado de publicacion es obligatorio")
    @Pattern(regexp = "ACTIVO|PAUSADO|BORRADOR", message = "El estado de publicacion debe ser ACTIVO, PAUSADO o BORRADOR")
    private String estadoPublicacion;

    @Size(max = 255, message = "La URL de la miniatura no puede superar los 255 caracteres")
    private String urlMiniatura;

    @DecimalMin(value = "0.00", message = "El cargo de revision adicional no puede ser negativo")
    private BigDecimal cargoRevisionAdicional;

    private Integer limiteRevisionesBase;

    private List<Long> etiquetaIds;
}
