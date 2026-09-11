package uteq.edu.ec.artisync.dto.seguridad.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload administrativo para sincronizar o reasignar el arbol de permisos de un rol.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncPermissionsRequest {

    @NotBlank(message = "El nombre del rol es obligatorio")
    private String roleName;

    @NotNull(message = "La lista de cÃ³digos de permisos es obligatoria")
    private List<String> permissionCodes;
}



