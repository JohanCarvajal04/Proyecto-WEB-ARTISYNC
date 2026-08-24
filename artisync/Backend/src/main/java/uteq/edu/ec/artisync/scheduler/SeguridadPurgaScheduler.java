package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fase 4 de mantenimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §7) —
 * corrige la anomalia A10: sesiones_usuario, tokens_recuperacion y
 * codigos_respaldo_2fa crecian sin ningun proceso de purga.
 *
 * Invoca sp_purgar_datos_seguridad, el unico PROCEDURE (no FUNCTION) del
 * modulo de seguridad: hace su propio COMMIT por lote en vez de una unica
 * transaccion de larga duracion sobre potencialmente millones de filas, que
 * bloquearia a VACUUM y haria crecer los dead tuples de toda la base
 * mientras dura.
 *
 * {@code Propagation.NOT_SUPPORTED} es obligatorio, no cosmetico: un
 * PROCEDURE con COMMIT interno falla con
 * {@code 2D000 invalid_transaction_termination} si Spring ya abrio una
 * transaccion antes de invocarlo (ver plan §0.3). JdbcTemplate por defecto
 * abre autocommit=true por sentencia cuando no hay una transaccion Spring
 * activa, que es exactamente el modo en que este PROCEDURE necesita
 * ejecutarse para poder hacer sus propios COMMIT/ROLLBACK internos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeguridadPurgaScheduler {

    /** Filas por lote dentro de sp_purgar_datos_seguridad; ver el parametro p_tamano_lote de la rutina. */
    private static final int TAMANO_LOTE = 1000;

    private final JdbcTemplate jdbcTemplate;

    // Corre a las 3:30 AM, media hora despues de VerificacionScheduler
    // (03:00) y SorteoScheduler, para no competir por E/S de disco con ellos.
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void purgarDatosSeguridad() {
        log.info("[SeguridadPurgaScheduler] Iniciando purga de sesiones expiradas, tokens de recuperación muertos y códigos de respaldo 2FA consumidos (lote={})", TAMANO_LOTE);
        try {
            jdbcTemplate.update("CALL sp_purgar_datos_seguridad(?)", TAMANO_LOTE);
            log.info("[SeguridadPurgaScheduler] Purga completada");
        } catch (Exception e) {
            // Best-effort: un fallo aqui no debe tumbar el proceso ni afectar
            // el trafico en vivo. La siguiente ejecucion (24h despues) vuelve
            // a intentarlo sobre lo que haya quedado sin purgar.
            log.error("[SeguridadPurgaScheduler] Error durante la purga de datos de seguridad: {}", e.getMessage(), e);
        }
    }
}
