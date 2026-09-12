package uteq.edu.ec.artisync.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.entity.legal.WithdrawalRequest;
import uteq.edu.ec.artisync.repository.legal.WithdrawalRequestRepository;
import uteq.edu.ec.artisync.service.shared.paypal.PayPalClient;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cierra el hueco que WithdrawalRequestServiceImpl deja explícito en su
 * comentario de BATCH_STATUS_EN_PROCESO ("PayPal la resuelve más tarde
 * (webhook o polling, fuera de alcance aquí)"): approve()/retry() solo
 * consultan PayPal una vez, en el momento de aprobar. Si PayPal responde
 * PENDING/UNCLAIMED/PROCESSING, la solicitud se queda en "Aprobado" para
 * siempre y retry() no aplica (solo funciona desde "Fallido") -- nada la
 * vuelve a mover ni a "Pagado" ni a "Fallido".
 *
 * No reutiliza los métodos privados de WithdrawalRequestServiceImpl (mismo
 * motivo documentado en PayPalReconciliationExecutorService): se mantiene
 * autocontenido para no arriesgar la lógica ya probada de approve/retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalPayoutReconciliationExecutorService {

    private static final String ESTADO_APROBADO = "Aprobado";
    private static final String ESTADO_PAGADO = "Pagado";
    private static final String ESTADO_FALLIDO = "Fallido";

    /**
     * transaction_status de un item de payout que PayPal todavía no resolvió
     * de forma definitiva. A diferencia del batch_status (que solo dice si
     * PayPal terminó de *intentar* cada item), esto sí refleja si el dinero
     * realmente llegó al creador.
     */
    private static final List<String> ITEM_STATUS_EN_PROCESO = List.of("PENDING", "UNCLAIMED", "ONHOLD");

    private final WithdrawalRequestRepository solicitudRetiroRepository;
    private final PayPalClient payPalClient;

    /**
     * Relee la solicitud con lock antes de actuar por si approve()/retry() ya
     * la resolvió mientras tanto, consulta el batch real en PayPal y aplica
     * el resultado: paga si el item ya está SUCCESS, deja constancia si sigue
     * en curso, o marca Fallido con el motivo si PayPal lo resolvió en contra
     * (FAILED, RETURNED, BLOCKED, etc.).
     *
     * @param idSolicitud identificador de la solicitud de retiro a reconciliar
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Auditable(accion = "RETIRO_RECONCILIAR", modulo = AuditModule.FINANZAS,
            correoActor = "'sistema:paypal'",
            entidad = "solicitudes_retiro", idEntidad = "#idSolicitud")
    public void reconciliar(Long idSolicitud) {
        WithdrawalRequest solicitud = solicitudRetiroRepository.findByIdParaActualizar(idSolicitud).orElse(null);
        if (solicitud == null || !ESTADO_APROBADO.equalsIgnoreCase(solicitud.getEstado())) {
            return;
        }

        String idBatch = solicitud.getIdPayoutPaypal();
        if (idBatch == null || idBatch.isBlank()) {
            log.warn("[WithdrawalPayoutReconciliationExecutorService] Retiro {} está Aprobado sin id de batch de PayPal; no hay nada que consultar",
                    solicitud.getIdSolicitud());
            return;
        }

        JsonNode respuesta;
        try {
            respuesta = payPalClient.callPayPal("/v1/payments/payouts/" + idBatch, HttpMethod.GET, null);
        } catch (Exception e) {
            log.error("[WithdrawalPayoutReconciliationExecutorService] Error consultando el batch {} (retiro {}) en PayPal: {}",
                    idBatch, solicitud.getIdSolicitud(), e.getMessage());
            return;
        }

        JsonNode primerItem = respuesta.path("items").path(0);
        String estadoItem = primerItem.path("transaction_status").asText();

        if ("SUCCESS".equals(estadoItem)) {
            solicitud.setEstado(ESTADO_PAGADO);
            solicitud.setFechaPago(LocalDateTime.now());
            solicitud.setMensajeError(null);
            solicitudRetiroRepository.save(solicitud);
            log.info("[WithdrawalPayoutReconciliationExecutorService] Retiro {} confirmado por reconciliación: PayPal completó el payout {}",
                    solicitud.getIdSolicitud(), idBatch);
        } else if (ITEM_STATUS_EN_PROCESO.contains(estadoItem)) {
            log.info("[WithdrawalPayoutReconciliationExecutorService] Retiro {} (batch {}) sigue en {} en PayPal; se reintenta en el próximo ciclo",
                    solicitud.getIdSolicitud(), idBatch, estadoItem);
        } else {
            String detalle = primerItem.path("errors").path("message").asText(estadoItem);
            solicitud.setEstado(ESTADO_FALLIDO);
            solicitud.setMensajeError("PayPal resolvió el payout como " + estadoItem + ": " + detalle);
            solicitudRetiroRepository.save(solicitud);
            log.warn("[WithdrawalPayoutReconciliationExecutorService] Retiro {} (batch {}) resuelto como fallido por PayPal: {}",
                    solicitud.getIdSolicitud(), idBatch, estadoItem);
        }
    }
}
