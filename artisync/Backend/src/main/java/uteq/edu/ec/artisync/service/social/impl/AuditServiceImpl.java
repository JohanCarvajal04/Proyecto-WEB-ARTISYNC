package uteq.edu.ec.artisync.service.social.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.entity.legal.TransaccionPago;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;
import uteq.edu.ec.artisync.service.social.AuditService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Implementación del servicio de auditoría.
 * RNF-13: Exportación CSV de transacciones de pago. Historial inmutable (solo lectura).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final TransaccionPagoRepository transaccionPagoRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarTransaccionesCreadorCsv(Long idCreadorPerfil) {
        // Obtenemos las transacciones navegando por la cadena:
        // TransaccionPago → PagoGarantia → Contrato → Pedido → Servicio → PerfilCreador
        List<TransaccionPago> transacciones = transaccionPagoRepository
                .findByCreadorPerfilId(idCreadorPerfil);

        StringBuilder csv = new StringBuilder();
        csv.append("ID_Transaccion,Tipo,Monto,Fecha_Ejecucion,ID_Pedido,ID_Pago,Estado_Fondos\n");

        for (TransaccionPago t : transacciones) {
            Long idPedido = null;
            String estadoFondos = "";
            Long idPago = null;

            if (t.getPago() != null) {
                idPago = t.getPago().getIdPago();
                estadoFondos = t.getPago().getEstadoFondos() != null ? t.getPago().getEstadoFondos() : "";
                if (t.getPago().getContrato() != null && t.getPago().getContrato().getPedido() != null) {
                    idPedido = t.getPago().getContrato().getPedido().getIdPedido();
                }
            }

            BigDecimal monto = t.getMonto() != null ? t.getMonto() : BigDecimal.ZERO;

            csv.append(String.format("%d,%s,%.2f,%s,%s,%s,%s\n",
                    t.getIdTransaccion(),
                    escapeCsv(t.getTipoTransaccion()),
                    monto,
                    t.getFechaEjecucion() != null ? t.getFechaEjecucion().toString() : "",
                    idPedido != null ? idPedido.toString() : "",
                    idPago != null ? idPago.toString() : "",
                    escapeCsv(estadoFondos)
            ));
        }

        log.info("CSV generado para creador {}: {} transacciones", idCreadorPerfil, transacciones.size());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Escapa caracteres especiales de CSV (comillas, comas). */
    private String escapeCsv(String valor) {
        if (valor == null) return "";
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
