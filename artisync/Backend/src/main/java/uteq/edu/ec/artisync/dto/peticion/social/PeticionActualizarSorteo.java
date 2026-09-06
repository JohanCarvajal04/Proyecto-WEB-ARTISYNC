package uteq.edu.ec.artisync.dto.peticion.social;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de petición para actualizar un sorteo existente.
 * RF-23: Solo título y descripción son siempre editables.
 * cantidadGanadores, premios y fechaCierre NO pueden modificarse si ya hay participantes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionActualizarSorteo {

    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String tituloSorteo;

    /** Opcional: si se envía, reemplaza la lista completa de premios. */
    @Size(max = 20, message = "No puedes definir más de 20 premios")
    private List<@NotBlank(message = "La descripción del premio no puede estar vacía")
            @Size(max = 255, message = "La descripción del premio no puede superar los 255 caracteres")
            String> premios;

    @Min(value = 1, message = "Debe haber al menos 1 ganador")
    private Integer cantidadGanadores;

    @Future(message = "La fecha de cierre debe ser futura")
    private LocalDateTime fechaCierre;
}
