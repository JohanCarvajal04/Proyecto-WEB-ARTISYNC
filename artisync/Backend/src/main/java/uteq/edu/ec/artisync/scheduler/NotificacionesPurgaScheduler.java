package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * H-08 (auditoría de estado 2026-08-26): notificaciones_sistema crecía sin
 * ningún proceso de purga. Es la única de las tres tablas de alto volumen
 * candidatas (junto a auditoria_eventos y mensajes) que se puede purgar sin
 * riesgo — ver la cabecera de {@code db/procs/sp_purgar_notificaciones.sql}
 * para por qué las otras dos quedan fuera de alcance.
 *
 * {@code Propagation.NOT_SUPPORTED} es obligatorio, no cosmético: igual que
 * en {@link SeguridadPurgaScheduler}, el PROCEDURE hace su propio COMMIT por
 * lote y falla con {@code 2D000 invalid_transaction_termination} si Spring
 * ya abrió una transacción antes de invocarlo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionesPurgaScheduler {

    /** Filas por lote; ver el parámetro p_tamano_lote de sp_purgar_notificaciones. */
    private static final int TAMANO_LOTE = 1000;

    /** Días de retención tras la lectura; ver el parámetro p_dias_retencion. */
    private static final int DIAS_RETENCION = 90;

    private final JdbcTemplate jdbcTemplate;

    // Corre a las 4:00 AM, después de VerificacionScheduler (03:00) y
    // SeguridadPurgaScheduler (03:30), para no competir por E/S de disco.
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void purgarNotificaciones() {
        log.info("[NotificacionesPurgaScheduler] Iniciando purga de notificaciones leídas con más de {} días (lote={})",
                DIAS_RETENCION, TAMANO_LOTE);
        try {
            jdbcTemplate.update("CALL sp_purgar_notificaciones(?, ?)", TAMANO_LOTE, DIAS_RETENCION);
            log.info("[NotificacionesPurgaScheduler] Purga completada");
        } catch (Exception e) {
            // Best-effort: un fallo aqui no debe tumbar el proceso ni afectar
            // el trafico en vivo. La siguiente ejecucion (24h despues) vuelve
            // a intentarlo sobre lo que haya quedado sin purgar.
            log.error("[NotificacionesPurgaScheduler] Error durante la purga de notificaciones: {}", e.getMessage(), e);
        }
    }
}
