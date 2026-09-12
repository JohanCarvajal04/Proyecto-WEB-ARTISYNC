package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.respuesta.legal.IntegrityVerificationResponse;
import uteq.edu.ec.artisync.service.legal.IContractService;

/**
 * Extraído de ContractIntegrityScheduler para que REQUIRES_NEW funcione de
 * verdad (mismo motivo documentado en RaffleExecutorService). A diferencia de
 * PayPalReconciliationExecutorService y RevisionTicketExpirationService, sí
 * reutiliza el servicio principal (IContractService.verifyHashIntegrity):
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
public class ContractIntegrityExecutorService {

    private final IContractService contratoServicio;

    /**
     * Recalcula el hash de contenido del contrato y lo compara contra el
     * almacenado; si difieren, registra un error en el log (no lanza excepción:
     * el barrido nocturno continúa con el resto de contratos).
     *
     * @param idContrato identificador del contrato ya firmado a verificar
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void verificar(Long idContrato) {
        IntegrityVerificationResponse resultado = contratoServicio.verifyHashIntegrity(idContrato);
        if (!resultado.isIntegro()) {
            log.error("[ContractIntegrityExecutorService] Discrepancia de integridad detectada en el contrato {}",
                    idContrato);
        }
    }
}
