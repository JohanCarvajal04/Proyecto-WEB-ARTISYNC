package uteq.edu.ec.artisync.dto.peticion.legal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Plantilla de acuerdo privada de un creador (V45). A diferencia de
 * {@link CreateContractTemplateRequest} (catálogo curado por ADMIN), no
 * expone versión legal ni "predeterminada": son conceptos de gobierno del
 * catálogo general que no aplican a una plantilla que solo usa su propio
 * dueño — el servicio los resuelve internamente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOwnAgreementTemplateRequest {

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 150, message = "El nombre de la plantilla no puede superar los 150 caracteres")
    private String nombrePlantilla;

    @NotBlank(message = "El cuerpo HTML de la plantilla es obligatorio")
    private String cuerpoHtmlPlantilla;
}
