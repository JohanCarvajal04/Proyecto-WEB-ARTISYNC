package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.respuesta.legal.IntegrityVerificationResponse;
import uteq.edu.ec.artisync.service.legal.IContractService;

/**
 * Extraído de ContratoIntegridadScheduler para que REQUIRES_NEW funcione de
 * verdad (mismo motivo documentado en RaffleExecutorService). A diferencia de
 * ReconciliacionPayPalEjecutorServicio y TicketRevisionExpiracionServicio, sí
 * reutiliza el servicio principal (IContractService.verificarIntegridadHash):
 * es una lectura sin efectos secundarios financieros, así que el riesgo que
 * justificaba autocontenerse en los otros dos casos no aplica aquí.
 *
 * Sin @Auditable a propósito: auditar cada pasada nocturna sin hallazgos
 * sería ruido en la bitácora inmutable (REQ-NF-013 es para eventos con
 * significado); solo se deja constancia (log ERROR) del caso raro de
 * discrepancia.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContratoIntegridadEjecutorServicio {

    private final IContractService contratoServicio;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void verificar(Long idContrato) {
        IntegrityVerificationResponse resultado = contratoServicio.verificarIntegridadHash(idContrato);
        if (!resultado.isIntegro()) {
            log.error("[ContratoIntegridadEjecutorServicio] Discrepancia de integridad detectada en el contrato {}",
                    idContrato);
        }
    }
}
