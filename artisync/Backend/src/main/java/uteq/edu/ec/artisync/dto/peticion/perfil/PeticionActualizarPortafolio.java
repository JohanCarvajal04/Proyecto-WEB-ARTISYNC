package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.Size;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload para editar los metadatos de un portafolio existente.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
public record PeticionActualizarPortafolio(
        Boolean esPublico,

        java.util.Map<String, String> opcionesPersonalizacion
) {
}



