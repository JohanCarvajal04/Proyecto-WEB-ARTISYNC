package uteq.edu.ec.artisync.dto.request.profile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload para registrar una nueva coleccion u agrupacion de obras.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 *
 * @param idPerfil id del perfil de creador dueño del portafolio
 * @param esPublico si el portafolio es visible públicamente
 * @param opcionesPersonalizacion opciones de personalización visual del portafolio
 */
public record CreatePortfolioRequest(
        @NotNull(message = "El ID del perfil es obligatorio")
        Long idPerfil,

        Boolean esPublico,

        java.util.Map<String, String> opcionesPersonalizacion
) {
}



