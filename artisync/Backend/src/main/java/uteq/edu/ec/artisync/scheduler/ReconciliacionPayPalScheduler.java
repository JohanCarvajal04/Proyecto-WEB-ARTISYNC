package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.entity.legal.EscrowPayment;
import uteq.edu.ec.artisync.repository.legal.EscrowPaymentRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REQ-NF-019: reconciliación activa contra la API de PayPal para pagos que
 * quedaron 'Pendiente' más allá de un umbral, por si el webhook nunca llegó
 * (entrega no garantizada por diseño: PayPal reintenta, pero no infinitamente).
 *
 * Requiere: @EnableScheduling en ArtisyncApplication (ya presente).
 *
 * A diferencia de los demás schedulers del paquete (umbral hardcodeado como
 * constante), este usa @Value porque el propio REQ-NF-019 exige
 * explícitamente un "umbral de tiempo configurable".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliacionPayPalScheduler {

    private final EscrowPaymentRepository pagoGarantiaRepository;
    private final ReconciliacionPayPalEjecutorServicio reconciliacionPayPalEjecutorServicio;

    @Value("${paypal.reconciliacion.umbral-minutos:30}")
    private int umbralMinutos;

    /**
     * Sin @Transactional aquí, mismo motivo que RaffleScheduler: cada pago se
     * reconcilia en su propia transacción (ReconciliacionPayPalEjecutorServicio,
     * REQUIRES_NEW) para que el fallo de uno no revierta a los demás.
     */
    @Scheduled(fixedRateString = "${paypal.reconciliacion.intervalo-ms:900000}") // 15 min
    public void reconciliarPagosPendientes() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(umbralMinutos);
        List<EscrowPayment> pendientes = pagoGarantiaRepository
                .findByEstadoFondosAndFechaActualizacionBefore("Pendiente", limite);

        if (pendientes.isEmpty()) {
            return;
        }

        log.info("[ReconciliacionPayPalScheduler] Reconciliando {} pago(s) pendiente(s) de más de {} min",
                pendientes.size(), umbralMinutos);

        for (EscrowPayment pago : pendientes) {
            try {
                reconciliacionPayPalEjecutorServicio.reconciliar(pago.getIdPago());
            } catch (Exception e) {
                log.error("[ReconciliacionPayPalScheduler] Error reconciliando pago {}: {}",
                        pago.getIdPago(), e.getMessage(), e);
            }
        }
    }
}
