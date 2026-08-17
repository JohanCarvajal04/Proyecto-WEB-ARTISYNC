package uteq.edu.ec.artisync.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.social.Sorteo;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.repository.social.SorteoRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

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
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final ObjectMapper objectMapper;

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
        // REQ-F-023: fn_seleccionar_ganadores_sorteo hace la seleccion aleatoria
        // (ORDER BY random()) y la actualizacion masiva de participantes+sorteo
        // en el motor, en vez de Collections.shuffle en Java seguido de un save()
        // por ganador. La notificacion en tiempo real permanece en Java.
        String resultadoJson = sorteoRepository.seleccionarGanadores(sorteo.getIdSorteo());
        JsonNode resultado = parseResultado(resultadoJson);
        String estado = resultado.get("estado").asText();
        JsonNode ganadoresNode = resultado.get("ganadores");

        if (ganadoresNode == null || !ganadoresNode.isArray() || ganadoresNode.isEmpty()) {
            log.info("[SorteoScheduler] Sorteo {} finalizado sin ganadores (estado={}).",
                    sorteo.getIdSorteo(), estado);
            return;
        }

        String tituloSorteo = resultado.hasNonNull("tituloSorteo")
                ? resultado.get("tituloSorteo").asText() : sorteo.getTituloSorteo();

        for (JsonNode ganadorNode : ganadoresNode) {
            Long idUsuario = ganadorNode.get("idUsuario").asLong();
            Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
            // Notificación en tiempo real al ganador vía WebSocket (M6)
            notificacionService.notificar(
                    usuario,
                    "SORTEO_GANADOR",
                    "¡Felicidades! Has ganado el sorteo: " + tituloSorteo
            );
        }

        log.info("[SorteoScheduler] Sorteo '{}' (ID={}) finalizado. {} ganador(es).",
                tituloSorteo, sorteo.getIdSorteo(), ganadoresNode.size());
    }

    private JsonNode parseResultado(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("Error al interpretar el resultado de fn_seleccionar_ganadores_sorteo", e);
        }
    }
}
