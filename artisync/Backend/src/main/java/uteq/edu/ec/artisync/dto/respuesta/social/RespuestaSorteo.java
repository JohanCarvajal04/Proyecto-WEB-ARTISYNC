package uteq.edu.ec.artisync.dto.respuesta.social;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para un sorteo.
 * RF-23: Incluye estado, participantes y ganadores (si estado = 'Finalizado').
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaSorteo {

    private Long idSorteo;
    private String tituloSorteo;
    private String descripcionPremios;
    private Integer cantidadGanadores;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaCierre;
    private String estadoSorteo;
    private Boolean requiereSeguidor;

    /** Información básica del creador del sorteo. */
    private Long idPerfilCreador;
    private String nombreCreador;

    /** Total de participantes inscritos. */
    private Long totalParticipantes;

    /** Indica si el usuario autenticado ya participa en este sorteo. */
    private boolean yoParticipo;

    /** Lista de ganadores — solo se incluye si estadoSorteo = 'Finalizado'. */
    private List<RespuestaGanador> ganadores;
}
