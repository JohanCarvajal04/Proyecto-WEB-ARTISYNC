package uteq.edu.ec.artisync.dto.peticion.legal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionActualizarPlantillaContrato {

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 150, message = "El nombre de la plantilla no puede superar los 150 caracteres")
    private String nombrePlantilla;

    @NotBlank(message = "La version legal es obligatoria")
    @Size(max = 50, message = "La version legal no puede superar los 50 caracteres")
    private String versionLegal;

    @NotBlank(message = "El cuerpo HTML de la plantilla es obligatorio")
    private String cuerpoHtmlPlantilla;

    private boolean esPredeterminada;

    private boolean activa;
}
