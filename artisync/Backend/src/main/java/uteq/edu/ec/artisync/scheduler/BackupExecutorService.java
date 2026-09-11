package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.entity.respaldo.BackupStatus;
import uteq.edu.ec.artisync.entity.respaldo.BackupOrigin;
import uteq.edu.ec.artisync.entity.respaldo.Backup;
import uteq.edu.ec.artisync.entity.respaldo.BackupSchedule;
import uteq.edu.ec.artisync.entity.respaldo.BackupType;
import uteq.edu.ec.artisync.repository.respaldo.BackupScheduleRepository;
import uteq.edu.ec.artisync.repository.respaldo.BackupRepository;

import java.time.LocalDateTime;

/**
 * Orquesta el inicio de un respaldo: crea la fila EN_PROGRESO y dispara el
 * trabajo async. Lo invocan tanto BackupScheduler (programado)
 * como BackupServiceImpl (disparo manual del admin).
 */
@Component
@RequiredArgsConstructor
public class BackupExecutorService {

    private final BackupRepository respaldoRepository;
    private final BackupScheduleRepository programacionRepository;
    private final AsyncBackupJobService trabajoAsincronoServicio;

    /** Disparo manual (admin autenticado, sin programación asociada). */
    public Backup iniciarManual(BackupType tipo, String correoSolicitante) {
        Backup respaldo = crearRespaldo(tipo, BackupOrigin.MANUAL, null, correoSolicitante);
        trabajoAsincronoServicio.ejecutar(respaldo.getIdRespaldo());
        return respaldo;
    }

    /** Disparo desde BackupScheduler. */
    public void iniciarDesdeProgramacion(BackupSchedule programacion) {
        Backup respaldo = crearRespaldo(
                programacion.getTipoRespaldo(), BackupOrigin.PROGRAMADO, programacion, "sistema:scheduler");

        // proxima_ejecucion se recalcula YA, no al terminar: si el respaldo
        // tarda más de 60s no debe volver a dispararse en el siguiente tick
        // del poller.
        LocalDateTime ahora = LocalDateTime.now();
        programacion.setProximaEjecucion(CronExpression.parse(programacion.getExpresionCron()).next(ahora));
        programacion.setUltimaEjecucion(ahora);
        programacion.setActualizadoEn(ahora);
        programacionRepository.save(programacion);

        trabajoAsincronoServicio.ejecutar(respaldo.getIdRespaldo());
    }

    private Backup crearRespaldo(BackupType tipo, BackupOrigin origen,
                                    BackupSchedule programacion, String correoSolicitante) {
        Backup.BackupBuilder builder = Backup.builder()
                .tipoRespaldo(tipo)
                .origen(origen)
                .programacion(programacion)
                .correoSolicitante(correoSolicitante)
                .fechaInicio(LocalDateTime.now());

        if (tipo == BackupType.INCREMENTAL) {
            Backup baseFull = respaldoRepository
                    .findTopByTipoRespaldoAndEstadoRespaldoOrderByFechaInicioDesc(BackupType.FULL, BackupStatus.COMPLETADO)
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe ningún respaldo FULL completado del que partir un INCREMENTAL"));

            // Corte = fecha del último respaldo de LA CADENA de ese FULL (el
            // incremental completado más reciente si existe, si no el FULL
            // mismo) -- no siempre "desde el FULL". Mantiene cada incremental
            // pequeño, el significado estándar de "incremental". Consecuencia:
            // restaurar a un punto en el tiempo requiere el FULL + TODA la
            // cadena de incrementales en orden (ver docs/despliegue/BACKUP.md).
            LocalDateTime corte = respaldoRepository
                    .findTopByIdRespaldoFullBaseOrderByFechaInicioDesc(baseFull.getIdRespaldo())
                    .map(Backup::getFechaInicio)
                    .orElse(baseFull.getFechaInicio());

            builder.idRespaldoFullBase(baseFull.getIdRespaldo()).fechaDesdeIncremental(corte);
        }

        return respaldoRepository.save(builder.build());
    }
}
