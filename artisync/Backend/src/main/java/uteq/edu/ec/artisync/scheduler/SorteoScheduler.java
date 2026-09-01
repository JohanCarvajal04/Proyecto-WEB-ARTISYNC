package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.entity.social.Sorteo;
import uteq.edu.ec.artisync.repository.social.SorteoRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tarea programada para el procesamiento automático de sorteos.
 * RF-23: Al llegar la fecha de cierre, el sistema ejecuta la selección aleatoria
 * de ganadores y los notifica en un máximo de 60 segundos.
 *
 * Requiere: @EnableScheduling en ArtisyncApplication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SorteoScheduler {

    private final SorteoRepository sorteoRepository;
    private final SorteoEjecutorServicio sorteoEjecutorServicio;

    /**
     * Se ejecuta cada 60 segundos.
     * Busca sorteos en estado "Activo" cuya fecha de cierre ya pasó y los finaliza.
     *
     * Sin @Transactional aquí: si todo el lote compartiera una única
     * transacción/conexión, una excepción en un sorteo la dejaría "aborted"
     * en Postgres hasta el COMMIT final -- el try/catch por elemento no la
     * libera, así que los sorteos siguientes fallarían en cascada y, al no
     * poder confirmar el método, Spring revertiría también los ya procesados
     * con éxito. Cada sorteo se procesa en su propia transacción
     * (SorteoEjecutorServicio.ejecutarSorteo, REQUIRES_NEW).
     */
    @Scheduled(fixedRate = 60_000)
    public void procesarSorteosCerrados() {
        List<Sorteo> sorteosPendientes = sorteoRepository
                .findByEstadoSorteoAndFechaCierreBefore("Activo", LocalDateTime.now());

        if (sorteosPendientes.isEmpty()) {
            return; // Nada que procesar
        }

        log.info("[SorteoScheduler] Procesando {} sorteo(s) cerrado(s)...", sorteosPendientes.size());

        for (Sorteo sorteo : sorteosPendientes) {
            try {
                sorteoEjecutorServicio.ejecutarSorteo(sorteo);
            } catch (Exception e) {
                log.error("[SorteoScheduler] Error al procesar sorteo {}: {}",
                        sorteo.getIdSorteo(), e.getMessage(), e);
            }
        }
    }
}
