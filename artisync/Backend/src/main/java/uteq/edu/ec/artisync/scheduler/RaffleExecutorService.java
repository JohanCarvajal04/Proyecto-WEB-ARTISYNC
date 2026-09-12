package uteq.edu.ec.artisync.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.entity.social.Raffle;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.repository.social.RaffleRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;

/**
 * Extraído de RaffleScheduler para que REQUIRES_NEW funcione de verdad: un
 * método @Transactional llamado desde dentro de la misma clase (this.metodo())
 * se salta el proxy de Spring AOP, así que la anotación se ignoraría en
 * silencio. Al vivir en un bean distinto, RaffleScheduler lo invoca a través
 * del proxy real y cada sorteo queda en su propia transacción.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RaffleExecutorService {

    private final RaffleRepository sorteoRepository;
    private final UserRepository usuarioRepository;
    private final NotificationService notificacionService;
    private final ObjectMapper objectMapper;

    /**
     * Cierra un sorteo: delega la selección aleatoria de ganadores en
     * {@code fn_seleccionar_ganadores_sorteo} (selección y actualización masiva
     * en el motor) y notifica en tiempo real a cada ganador vía WebSocket.
     *
     * @param sorteo sorteo a cerrar y resolver
     * @throws IllegalStateException si el resultado del procedimiento no se puede interpretar
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ejecutarSorteo(Raffle sorteo) {
        // REQ-F-023: fn_seleccionar_ganadores_sorteo hace la seleccion aleatoria
        // (ORDER BY random()) y la actualizacion masiva de participantes+sorteo
        // en el motor, en vez de Collections.shuffle en Java seguido de un save()
        // por ganador. La notificacion en tiempo real permanece en Java.
        String resultadoJson = sorteoRepository.seleccionarGanadores(sorteo.getIdSorteo());
        JsonNode resultado = parseResultado(resultadoJson);
        String estado = resultado.get("estado").asText();
        JsonNode ganadoresNode = resultado.get("ganadores");

        if (ganadoresNode == null || !ganadoresNode.isArray() || ganadoresNode.isEmpty()) {
            log.info("[RaffleScheduler] Raffle {} finalizado sin ganadores (estado={}).",
                    sorteo.getIdSorteo(), estado);
            return;
        }

        String tituloSorteo = resultado.hasNonNull("tituloSorteo")
                ? resultado.get("tituloSorteo").asText() : sorteo.getTituloSorteo();

        for (JsonNode ganadorNode : ganadoresNode) {
            Long idUsuario = ganadorNode.get("idUsuario").asLong();
            User usuario = usuarioRepository.getReferenceById(idUsuario);
            String descripcionPremio = ganadorNode.hasNonNull("descripcionPremio")
                    ? ganadorNode.get("descripcionPremio").asText() : null;
            String mensaje = descripcionPremio != null
                    ? "¡Felicidades! Has ganado el premio '" + descripcionPremio + "' en el sorteo: " + tituloSorteo
                    : "¡Felicidades! Has ganado el sorteo: " + tituloSorteo;
            // Notificación en tiempo real al ganador vía WebSocket (M6)
            notificacionService.notificar(usuario, "SORTEO_GANADOR", mensaje);
        }

        log.info("[RaffleScheduler] Raffle '{}' (ID={}) finalizado. {} ganador(es).",
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
