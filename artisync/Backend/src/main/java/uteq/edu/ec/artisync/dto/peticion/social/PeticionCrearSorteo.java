package uteq.edu.ec.artisync.dto.peticion.social;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de petición para crear un nuevo sorteo.
 * RF-23: Campos obligatorios del sorteo configurable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearSorteo {

    @NotBlank(message = "El título del sorteo es obligatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String tituloSorteo;

    @NotBlank(message = "La descripción de los premios es obligatoria")
    private String descripcionPremios;

    @NotNull(message = "La cantidad de ganadores es obligatoria")
    @Min(value = 1, message = "Debe haber al menos 1 ganador")
    private Integer cantidadGanadores;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La fecha de inicio debe ser futura")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de cierre es obligatoria")
    @Future(message = "La fecha de cierre debe ser futura")
    private LocalDateTime fechaCierre;

    private boolean requiereSeguidor;
}
