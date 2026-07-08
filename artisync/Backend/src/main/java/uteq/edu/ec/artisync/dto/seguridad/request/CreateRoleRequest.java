package uteq.edu.ec.artisync.dto.seguridad.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 50, message = "El nombre del rol no puede superar los 50 caracteres")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "El nombre del rol debe estar en mayúsculas y solo contener letras, números y guiones bajos")
    private String nombreRol;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcionRol;

    private List<String> permisosIniciales;
}
