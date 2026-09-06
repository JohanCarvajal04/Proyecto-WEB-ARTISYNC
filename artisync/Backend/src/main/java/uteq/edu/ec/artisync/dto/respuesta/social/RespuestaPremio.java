package uteq.edu.ec.artisync.dto.respuesta.social;

import lombok.*;

/**
 * DTO de respuesta para un premio individual de un sorteo.
 * RF-23: permite mostrar cada premio por separado junto con su ganador
 * (si el sorteo ya finalizó y ese premio fue asignado).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaPremio {

    private Long idPremio;
    private String descripcionPremio;
    private Integer orden;

    /** Nulo si el sorteo no ha finalizado, o si por falta de participantes este premio no se asignó. */
    private RespuestaGanador ganador;
}
