package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.entity.legal.Contract;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;

import java.util.List;

/**
 * REQ-NF-020: barrido nocturno que recalcula el hash de contenido de todo
 * contrato ya firmado por ambas partes y registra en el log cualquier
 * discrepancia. A las 4:30 AM, después de VerificacionScheduler (3:00),
 * SeguridadPurgaScheduler (3:30) y NotificacionesPurgaScheduler (4:00) --
 * mismo criterio de escalonamiento por horario que ya usan esos tres.
 *
 * Requiere: @EnableScheduling en ArtisyncApplication (ya presente).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContratoIntegridadScheduler {

    private final ContractRepository contratoRepository;
    private final ContratoIntegridadEjecutorServicio contratoIntegridadEjecutorServicio;

    @Scheduled(cron = "${contrato.integridad.cron:0 30 4 * * *}")
    public void verificarIntegridadDeTodos() {
        List<Contract> contratosFirmados = contratoRepository.findByHashContenidoIsNotNull();

        if (contratosFirmados.isEmpty()) {
            return;
        }

        log.info("[ContratoIntegridadScheduler] Verificando la integridad de {} contrato(s) firmado(s)",
                contratosFirmados.size());

        for (Contract contrato : contratosFirmados) {
            try {
                contratoIntegridadEjecutorServicio.verificar(contrato.getIdContrato());
            } catch (Exception e) {
                log.error("[ContratoIntegridadScheduler] Error verificando el contrato {}: {}",
                        contrato.getIdContrato(), e.getMessage(), e);
            }
        }
    }
}
