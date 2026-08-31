package uteq.edu.ec.artisync.dto.peticion.legal;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Filtros del panel de supervisión de Pagos y Garantías (Escrow), enlazados
 * directo desde los query params de {@code GET /api/v1/admin/pagos-garantia}.
 */
@Data
public class FiltroPagoGarantia {

    /** "Pendiente" | "Retenido" | "Liberado" (ver PagoServicioImpl). */
    private String estadoFondos;

    private Long idPerfilCreador;

    private Long idUsuarioCliente;

    private LocalDateTime desde;

    private LocalDateTime hasta;
}
