package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record RespuestaPagoGarantiaDetalle(
        Long idPago,
        Long idContrato,
        Long idPedido,
        String tituloServicio,
        Long idUsuarioCliente,
        String nombreCliente,
        String correoCliente,
        Long idPerfilCreador,
        String nombreCreador,
        String idOrdenPaypal,
        BigDecimal montoRetenido,
        String estadoFondos,
        LocalDateTime fechaFormalizacion,
        List<RespuestaTransaccionPago> transacciones
) {
}
