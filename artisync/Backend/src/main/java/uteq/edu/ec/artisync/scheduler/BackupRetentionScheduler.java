package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.config.BackupProperties;
import uteq.edu.ec.artisync.entity.respaldo.BackupStatus;
import uteq.edu.ec.artisync.entity.respaldo.Backup;
import uteq.edu.ec.artisync.entity.respaldo.BackupType;
import uteq.edu.ec.artisync.repository.respaldo.BackupRepository;
import uteq.edu.ec.artisync.service.respaldo.impl.BackupFileStorage;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Purga diaria de respaldos vencidos según su retención (REQ-NF-024). Mismo
 * patrón de job de mantenimiento diario best-effort que SecurityPurgeScheduler
 * -- corre a las 4:00 AM, después de esa (3:30), para no competir por I/O de
 * disco con ella ni con VerificationScheduler/RaffleScheduler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupRetentionScheduler {

    private final BackupRepository respaldoRepository;
    private final BackupProperties respaldoProperties;
    private final BackupFileStorage storage;

    /**
     * Elimina (registro + archivo en disco) los respaldos {@code COMPLETADO}
     * cuya retención ya venció y que son seguros de borrar (ver {@link #esSeguroEliminar}).
     * Corre a las 4:00 AM; cada respaldo se procesa en su propio try/catch para
     * que un fallo puntual no bloquee el resto del barrido.
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void purgarRespaldosVencidos() {
        for (Backup respaldo : respaldoRepository.findByEstadoRespaldo(BackupStatus.COMPLETADO)) {
            try {
                if (haVencido(respaldo) && esSeguroEliminar(respaldo)) {
                    if (respaldo.getRutaArchivo() != null) {
                        storage.eliminar(Path.of(respaldo.getRutaArchivo()));
                    }
                    respaldoRepository.delete(respaldo);
                    log.info("[BackupRetentionScheduler] Backup {} eliminado (retención vencida)", respaldo.getIdRespaldo());
                }
            } catch (Exception e) {
                log.error("[BackupRetentionScheduler] Error al purgar el respaldo {}: {}",
                        respaldo.getIdRespaldo(), e.getMessage(), e);
            }
        }
    }

    private boolean haVencido(Backup respaldo) {
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
     * (BackupServiceImpl#eliminar): un admin tampoco puede borrar a mano
     * un FULL del que dependen incrementales vivos.
     */
    public boolean esSeguroEliminar(Backup respaldo) {
        if (respaldo.getTipoRespaldo() == BackupType.INCREMENTAL) {
            return true;
        }
        return !respaldoRepository.existsByIdRespaldoFullBaseAndEstadoRespaldo(
                respaldo.getIdRespaldo(), BackupStatus.COMPLETADO);
    }
}
