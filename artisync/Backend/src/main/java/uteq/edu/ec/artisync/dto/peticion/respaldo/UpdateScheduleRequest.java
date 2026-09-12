package uteq.edu.ec.artisync.dto.peticion.respaldo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import uteq.edu.ec.artisync.entity.respaldo.BackupType;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload para modificar la frecuencia o configuracion de respaldos programados.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
public class UpdateScheduleRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotNull(message = "El tipo de respaldo es obligatorio")
    private BackupType tipoRespaldo;

    @NotBlank(message = "La expresión cron es obligatoria")
    private String expresionCron;

    @NotNull(message = "La retención en días es obligatoria")
    @Min(value = 1, message = "La retención debe ser de al menos 1 día")
    @Max(value = 3650, message = "La retención no puede superar los 3650 días")
    private Integer retencionDias;
}



