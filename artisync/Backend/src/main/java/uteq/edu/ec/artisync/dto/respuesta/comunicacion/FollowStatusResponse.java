package uteq.edu.ec.artisync.dto.respuesta.comunicacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Estado booleano que indica si el usuario actual sigue al creador consultado.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatusResponse {
    private Boolean esSeguidor;
    private Long totalSeguidores;
    private Boolean esPropioPerfil;
}



