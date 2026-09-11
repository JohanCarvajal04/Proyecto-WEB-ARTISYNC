package uteq.edu.ec.artisync.dto.peticion.legal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uteq.edu.ec.artisync.dto.peticion.comun.FiltroRangoFechas;

/**
 * Filtros del panel de supervisión de Pagos y Garantías (Escrow), enlazados
 * directo desde los query params de {@code GET /api/v1/admin/pagos-garantia}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EscrowPaymentFilter extends FiltroRangoFechas {

    /** "Pendiente" | "Retenido" | "Liberado" (ver PaymentServiceImpl). */
    private String estadoFondos;

    private Long idPerfilCreador;

    private Long idUsuarioCliente;
}
