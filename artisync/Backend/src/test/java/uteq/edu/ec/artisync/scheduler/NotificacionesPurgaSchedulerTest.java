package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.*;

/**
 * H-08 (auditoría de estado 2026-08-26): {@link NotificacionesPurgaScheduler}
 * invoca sp_purgar_notificaciones (un PROCEDURE con COMMIT interno) vía
 * JdbcTemplate, fuera de cualquier transacción Spring.
 */
@ExtendWith(MockitoExtension.class)
class NotificacionesPurgaSchedulerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private NotificacionesPurgaScheduler scheduler;

    @Test
    void purgarNotificaciones_invocaElProcedureConLoteYRetencionConfigurados() {
        scheduler.purgarNotificaciones();

        verify(jdbcTemplate).update("CALL sp_purgar_notificaciones(?, ?)", 1000, 90);
    }

    @Test
    void purgarNotificaciones_noPropagaLaExcepcion_siFallaLaConexion() {
        // Best-effort: un fallo de la purga (DB no disponible, timeout) no
        // debe tumbar el hilo del scheduler ni afectar al trafico en vivo. La
        // siguiente ejecucion programada vuelve a intentarlo.
        doThrow(new TransientDataAccessResourceException("simulado: conexión no disponible"))
                .when(jdbcTemplate).update(anyString(), any(Object[].class));

        scheduler.purgarNotificaciones();

        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }
}
