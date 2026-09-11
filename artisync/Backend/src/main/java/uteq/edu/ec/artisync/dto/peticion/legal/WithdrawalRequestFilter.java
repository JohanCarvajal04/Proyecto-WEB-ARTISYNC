package uteq.edu.ec.artisync.dto.peticion.legal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uteq.edu.ec.artisync.dto.peticion.comun.FiltroRangoFechas;

/**
 * Filtros de la cola de revisión de retiros, enlazados directo desde los
 * query params de {@code GET /api/v1/admin/retiros}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WithdrawalRequestFilter extends FiltroRangoFechas {

    /** "Pendiente" | "Aprobado" | "Pagado" | "Rechazado" | "Fallido". */
    private String estado;

    private Long idUsuarioCreador;
}
