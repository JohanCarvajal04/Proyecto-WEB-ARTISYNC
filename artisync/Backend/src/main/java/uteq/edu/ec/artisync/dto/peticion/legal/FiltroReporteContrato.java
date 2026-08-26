package uteq.edu.ec.artisync.dto.peticion.legal;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

/** Filtros del reporte de contratos, ligados con @ModelAttribute (mismo estilo que FiltroReporteFinanciero). */
@Data
public class FiltroReporteContrato {

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime desde;

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime hasta;

    /** Filtra por el perfil del creador dueño del servicio contratado (servicio.perfil.idPerfil). */
    private Long idPerfilCreador;

    /** Si es true, solo contratos con ambas firmas; si es false, solo los pendientes; si se omite, todos. */
    private Boolean soloFirmados;
}
