package uteq.edu.ec.artisync.dto.peticion.legal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos para que un creador edite su propia plantilla de contrato privada (V45). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOwnAgreementTemplateRequest {

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 150, message = "El nombre de la plantilla no puede superar los 150 caracteres")
    private String nombrePlantilla;

    @NotBlank(message = "El cuerpo HTML de la plantilla es obligatorio")
    private String cuerpoHtmlPlantilla;

    private boolean activa;
}
