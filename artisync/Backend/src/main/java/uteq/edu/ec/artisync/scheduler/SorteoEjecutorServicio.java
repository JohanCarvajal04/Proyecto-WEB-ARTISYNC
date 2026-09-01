package uteq.edu.ec.artisync.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.social.Sorteo;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.repository.social.SorteoRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

/**
 * Extraído de SorteoScheduler para que REQUIRES_NEW funcione de verdad: un
 * método @Transactional llamado desde dentro de la misma clase (this.metodo())
 * se salta el proxy de Spring AOP, así que la anotación se ignoraría en
 * silencio. Al vivir en un bean distinto, SorteoScheduler lo invoca a través
 * del proxy real y cada sorteo queda en su propia transacción.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SorteoEjecutorServicio {

    private final SorteoRepository sorteoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ejecutarSorteo(Sorteo sorteo) {
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
