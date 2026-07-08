package uteq.edu.ec.artisync.dto.seguridad.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcionRol;
}
