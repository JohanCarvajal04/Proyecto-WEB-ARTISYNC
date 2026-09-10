package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.entity.respaldo.RespaldoProgramacion;
import uteq.edu.ec.artisync.repository.respaldo.RespaldoProgramacionRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tarea programada que dispara los respaldos cuya programación está vencida.
 * Requiere @EnableScheduling en ArtisyncApplication (ya presente).
 *
 * Se ejecuta cada 60 segundos, mismo patrón que SorteoScheduler: sin
 * @Transactional en el bucle (cada respaldo se procesa en su propia unidad de
 * trabajo dentro de RespaldoEjecutorServicio) y try/catch por elemento, para
 * que un fallo en una programación no bloquee el resto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RespaldoProgramacionScheduler {

    private final RespaldoProgramacionRepository programacionRepository;
    private final RespaldoEjecutorServicio respaldoEjecutorServicio;

    @Scheduled(fixedRate = 60_000)
    public void procesarProgramacionesPendientes() {
        List<RespaldoProgramacion> pendientes =
                programacionRepository.findByActivoTrueAndProximaEjecucionLessThanEqual(LocalDateTime.now());

        if (pendientes.isEmpty()) {
            return;
        }

        log.info("[RespaldoProgramacionScheduler] Procesando {} programación(es) vencida(s)...", pendientes.size());

        for (RespaldoProgramacion programacion : pendientes) {
            try {
                respaldoEjecutorServicio.iniciarDesdeProgramacion(programacion);
            } catch (Exception e) {
                log.error("[RespaldoProgramacionScheduler] Error al iniciar la programación {}: {}",
                        programacion.getIdProgramacion(), e.getMessage(), e);
            }
        }
    }
}
