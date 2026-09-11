package uteq.edu.ec.artisync.dto.peticion.respaldo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Comando para activar o pausar un respaldo automatico programado.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
public class ChangeScheduleStatusRequest {

    @NotNull(message = "El estado activo/inactivo es obligatorio")
    private Boolean activo;
}



