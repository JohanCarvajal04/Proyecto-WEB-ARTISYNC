package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.entity.legal.WithdrawalRequest;
import uteq.edu.ec.artisync.repository.legal.WithdrawalRequestRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REQ-F-024: reconciliación activa contra la API de PayPal para retiros que
 * quedaron "Aprobado" (payout PENDING/UNCLAIMED/PROCESSING) más allá de un
 * umbral, porque retry() solo aplica desde "Fallido" y nada más vuelve a
 * consultar un payout que PayPal no resolvió de inmediato.
 *
 * Mismo patrón que PayPalReconciliationScheduler (escrow): cada solicitud se
 * reconcilia en su propia transacción vía
 * WithdrawalPayoutReconciliationExecutorService (REQUIRES_NEW), sin
 * @Transactional aquí, para que el fallo de una no revierta a las demás.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalPayoutReconciliationScheduler {

    private static final String ESTADO_APROBADO = "Aprobado";

    private final WithdrawalRequestRepository solicitudRetiroRepository;
    private final WithdrawalPayoutReconciliationExecutorService reconciliacionRetiroEjecutorServicio;

    @Value("${paypal.reconciliacion-retiros.umbral-minutos:30}")
    private int umbralMinutos;

    @Scheduled(fixedRateString = "${paypal.reconciliacion-retiros.intervalo-ms:900000}") // 15 min
    public void reconciliarRetirosAprobados() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(umbralMinutos);
        List<WithdrawalRequest> aprobados = solicitudRetiroRepository
                .findByEstadoAndFechaDecisionBefore(ESTADO_APROBADO, limite);

        if (aprobados.isEmpty()) {
            return;
        }

        log.info("[WithdrawalPayoutReconciliationScheduler] Reconciliando {} retiro(s) Aprobado(s) de más de {} min sin resolver",
                aprobados.size(), umbralMinutos);

        for (WithdrawalRequest solicitud : aprobados) {
            try {
                reconciliacionRetiroEjecutorServicio.reconciliar(solicitud.getIdSolicitud());
            } catch (Exception e) {
                log.error("[WithdrawalPayoutReconciliationScheduler] Error reconciliando retiro {}: {}",
                        solicitud.getIdSolicitud(), e.getMessage(), e);
            }
        }
    }
}
