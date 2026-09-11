package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.config.BackupProperties;
import uteq.edu.ec.artisync.entity.respaldo.EstadoRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.Respaldo;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;
import uteq.edu.ec.artisync.repository.respaldo.RespaldoRepository;
import uteq.edu.ec.artisync.service.respaldo.impl.RespaldoArchivoStorage;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Purga diaria de respaldos vencidos según su retención (REQ-NF-024). Mismo
 * patrón de job de mantenimiento diario best-effort que SeguridadPurgaScheduler
 * -- corre a las 4:00 AM, después de esa (3:30), para no competir por I/O de
 * disco con ella ni con VerificacionScheduler/SorteoScheduler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RespaldoRetencionScheduler {

    private final RespaldoRepository respaldoRepository;
    private final BackupProperties respaldoProperties;
    private final RespaldoArchivoStorage storage;

    @Scheduled(cron = "0 0 4 * * *")
    public void purgarRespaldosVencidos() {
        for (Respaldo respaldo : respaldoRepository.findByEstadoRespaldo(EstadoRespaldo.COMPLETADO)) {
            try {
                if (haVencido(respaldo) && esSeguroEliminar(respaldo)) {
                    if (respaldo.getRutaArchivo() != null) {
                        storage.eliminar(Path.of(respaldo.getRutaArchivo()));
                    }
                    respaldoRepository.delete(respaldo);
                    log.info("[RespaldoRetencionScheduler] Respaldo {} eliminado (retención vencida)", respaldo.getIdRespaldo());
                }
            } catch (Exception e) {
                log.error("[RespaldoRetencionScheduler] Error al purgar el respaldo {}: {}",
                        respaldo.getIdRespaldo(), e.getMessage(), e);
            }
        }
    }

    private boolean haVencido(Respaldo respaldo) {
        int dias = respaldo.getProgramacion() != null
                ? respaldo.getProgramacion().getRetencionDias()
                : respaldoProperties.getRetencionDiasManual();
        return respaldo.getFechaInicio().isBefore(LocalDateTime.now().minusDays(dias));
    }

    /**
     * Un INCREMENTAL siempre es seguro de borrar solo. Un FULL NO se borra
     * mientras existan INCREMENTAL COMPLETADO que dependan de él: sin la FULL
     * esos incrementales quedan huérfanos e irrecuperables. Consecuencia
     * documentada (ver docs/despliegue/BACKUP.md): la retención de una FULL
     * es un MÍNIMO, no un máximo estricto -- puede sobrevivir más allá de su
     * retención configurada mientras algún incremental la necesite.
     *
     * Reutilizado también por la eliminación manual del controlador
     * (RespaldoServicioImpl#eliminar): un admin tampoco puede borrar a mano
     * un FULL del que dependen incrementales vivos.
     */
    public boolean esSeguroEliminar(Respaldo respaldo) {
        if (respaldo.getTipoRespaldo() == TipoRespaldo.INCREMENTAL) {
            return true;
        }
        return !respaldoRepository.existsByIdRespaldoFullBaseAndEstadoRespaldo(
                respaldo.getIdRespaldo(), EstadoRespaldo.COMPLETADO);
    }
}
