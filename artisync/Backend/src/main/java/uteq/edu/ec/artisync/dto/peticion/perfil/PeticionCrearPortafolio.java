package uteq.edu.ec.artisync.dto.peticion.perfil;

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
 */
public record PeticionCrearPortafolio(
        @NotNull(message = "El ID del perfil es obligatorio")
        Long idPerfil,

        Boolean esPublico,

        java.util.Map<String, String> opcionesPersonalizacion
) {
}



