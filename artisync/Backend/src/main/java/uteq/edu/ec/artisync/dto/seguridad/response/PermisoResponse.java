package uteq.edu.ec.artisync.dto.seguridad.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de los privilegios atomicos asignados a roles.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermisoResponse {

    private Long idPermiso;
    private String nombrePermiso;
    private String moduloAplicacion;
}



