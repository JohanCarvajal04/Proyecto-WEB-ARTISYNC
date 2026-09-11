package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload del administrador para aprobar o rechazar la validacion KYC de un usuario.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
public record PeticionDecisionVerificacion(
        @NotNull(message = "El nuevo estado de verificaciÃ³n es obligatorio")
        Long idEstadoVerificacion,

        @Size(max = 500, message = "La nota no puede superar los 500 caracteres")
        String notaModerador
) {
}



