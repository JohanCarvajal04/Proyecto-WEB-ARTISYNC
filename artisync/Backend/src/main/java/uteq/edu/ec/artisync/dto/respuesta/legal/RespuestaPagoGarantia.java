package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila del panel de supervisión de Pagos y Garantías (Escrow) del Auditor
 * Financiero.
 */
@Builder
public record RespuestaPagoGarantia(
        Long idPago,
        Long idContrato,
        Long idPedido,
        String tituloServicio,
        Long idUsuarioCliente,
        String nombreCliente,
        Long idPerfilCreador,
        String nombreCreador,
        String idOrdenPaypal,
        BigDecimal montoRetenido,
        String estadoFondos,
        LocalDateTime fechaFormalizacion
) {
}
