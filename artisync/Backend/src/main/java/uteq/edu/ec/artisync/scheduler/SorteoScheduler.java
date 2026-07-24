package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.entity.social.ParticipanteSorteo;
import uteq.edu.ec.artisync.entity.social.Sorteo;
import uteq.edu.ec.artisync.repository.social.ParticipanteSorteoRepository;
import uteq.edu.ec.artisync.repository.social.SorteoRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
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
    private final ParticipanteSorteoRepository participanteSorteoRepository;
    private final NotificacionService notificacionService;

    /**
     * Se ejecuta cada 60 segundos.
     * Busca sorteos en estado "Activo" cuya fecha de cierre ya pasó y los finaliza.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void procesarSorteosCerrados() {
        List<Sorteo> sorteosPendientes = sorteoRepository
                .findByEstadoSorteoAndFechaCierreBefore("Activo", LocalDateTime.now());

        if (sorteosPendientes.isEmpty()) {
            return; // Nada que procesar
        }

        log.info("[SorteoScheduler] Procesando {} sorteo(s) cerrado(s)...", sorteosPendientes.size());

        for (Sorteo sorteo : sorteosPendientes) {
            try {
                ejecutarSorteo(sorteo);
            } catch (Exception e) {
                log.error("[SorteoScheduler] Error al procesar sorteo {}: {}",
                        sorteo.getIdSorteo(), e.getMessage(), e);
            }
        }
    }

    // =========================================================================
    // Lógica interna de selección de ganadores
    // =========================================================================

    private void ejecutarSorteo(Sorteo sorteo) {
        List<ParticipanteSorteo> participantes = participanteSorteoRepository
                .findBySorteoIdSorteoAndEsGanadorFalse(sorteo.getIdSorteo());

        if (participantes.isEmpty()) {
            sorteo.setEstadoSorteo("Finalizado_Sin_Participantes");
            sorteoRepository.save(sorteo);
            log.info("[SorteoScheduler] Sorteo {} finalizado sin participantes.", sorteo.getIdSorteo());
            return;
        }

        // Selección aleatoria criptográficamente segura (RF-23)
        Collections.shuffle(participantes, new SecureRandom());
        int cantidad = Math.min(sorteo.getCantidadGanadores(), participantes.size());
        List<ParticipanteSorteo> ganadores = participantes.subList(0, cantidad);

        LocalDateTime ahora = LocalDateTime.now();
        for (ParticipanteSorteo ganador : ganadores) {
            ganador.setEsGanador(true);
            ganador.setFechaNotificacionPremio(ahora);
            participanteSorteoRepository.save(ganador);

            // Notificación en tiempo real al ganador vía WebSocket (M6)
            notificacionService.notificar(
                    ganador.getUsuario(),
                    "SORTEO_GANADOR",
                    "¡Felicidades! Has ganado el sorteo: " + sorteo.getTituloSorteo()
            );
        }

        sorteo.setEstadoSorteo("Finalizado");
        sorteoRepository.save(sorteo);

        log.info("[SorteoScheduler] Sorteo '{}' (ID={}) finalizado. {} ganador(es) de {} participante(s).",
                sorteo.getTituloSorteo(), sorteo.getIdSorteo(), cantidad, participantes.size());
    }
}
