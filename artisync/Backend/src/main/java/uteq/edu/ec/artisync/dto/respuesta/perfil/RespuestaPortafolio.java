package uteq.edu.ec.artisync.dto.respuesta.perfil;

import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de una coleccion u obra especifica del creador.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
@Builder
public record RespuestaPortafolio(
        Long idPortafolio,
        Long idPerfil,
        LocalDateTime fechaCreacion,
        Integer totalVisitasAcumuladas,
        Boolean esPublico,
        java.util.Map<String, String> opcionesPersonalizacion
) {
}



