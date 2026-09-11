package uteq.edu.ec.artisync.dto.peticion.respaldo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;

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
public class PeticionActualizarProgramacion {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotNull(message = "El tipo de respaldo es obligatorio")
    private TipoRespaldo tipoRespaldo;

    @NotBlank(message = "La expresiÃ³n cron es obligatoria")
    private String expresionCron;

    @NotNull(message = "La retenciÃ³n en dÃ­as es obligatoria")
    @Min(value = 1, message = "La retenciÃ³n debe ser de al menos 1 dÃ­a")
    @Max(value = 3650, message = "La retenciÃ³n no puede superar los 3650 dÃ­as")
    private Integer retencionDias;
}



