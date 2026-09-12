package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.entity.respaldo.BackupSchedule;
import uteq.edu.ec.artisync.repository.respaldo.BackupScheduleRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tarea programada que dispara los respaldos cuya programación está vencida.
 * Requiere {@code @EnableScheduling} en ArtisyncApplication (ya presente).
 *
 * Se ejecuta cada 60 segundos, mismo patrón que RaffleScheduler: sin
 * {@code @Transactional} en el bucle (cada respaldo se procesa en su propia unidad de
 * trabajo dentro de BackupExecutorService) y try/catch por elemento, para
 * que un fallo en una programación no bloquee el resto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupScheduleRepository programacionRepository;
    private final BackupExecutorService respaldoEjecutorServicio;

    /**
     * Dispara, a través de {@link BackupExecutorService}, cada programación
     * activa cuya {@code proximaEjecucion} ya venció. Cada programación se
     * procesa en su propio try/catch para que un fallo no bloquee el resto.
     */
    @Scheduled(fixedRate = 60_000)
    public void procesarProgramacionesPendientes() {
        List<BackupSchedule> pendientes =
                programacionRepository.findByActivoTrueAndProximaEjecucionLessThanEqual(LocalDateTime.now());

        if (pendientes.isEmpty()) {
            return;
        }

        log.info("[BackupScheduler] Procesando {} programación(es) vencida(s)...", pendientes.size());

        for (BackupSchedule programacion : pendientes) {
            try {
                respaldoEjecutorServicio.iniciarDesdeProgramacion(programacion);
            } catch (Exception e) {
                log.error("[BackupScheduler] Error al iniciar la programación {}: {}",
                        programacion.getIdProgramacion(), e.getMessage(), e);
            }
        }
    }
}
