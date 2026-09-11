package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload inicial para configurar el perfil publico de un creador recien registrado.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
public record PeticionCrearPerfil(
        @NotNull(message = "El ID del usuario es obligatorio")
        Long idUsuario,

        @Size(max = 500, message = "La biografÃ­a no puede superar los 500 caracteres")
        String biografia,

        @Size(max = 255, message = "La URL de red social no puede superar los 255 caracteres")
        String urlRedSocial,

        @Size(max = 150, message = "El tÃ­tulo profesional no puede superar los 150 caracteres")
        String tituloProfesional
) {
}



