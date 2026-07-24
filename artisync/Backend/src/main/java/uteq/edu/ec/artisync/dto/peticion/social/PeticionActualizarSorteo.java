package uteq.edu.ec.artisync.dto.peticion.social;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de petición para actualizar un sorteo existente.
 * RF-23: Solo título y descripción son siempre editables.
 * cantidadGanadores y fechaCierre NO pueden modificarse si ya hay participantes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionActualizarSorteo {

    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String tituloSorteo;

    private String descripcionPremios;

    @Min(value = 1, message = "Debe haber al menos 1 ganador")
    private Integer cantidadGanadores;

    @Future(message = "La fecha de cierre debe ser futura")
    private LocalDateTime fechaCierre;
}
