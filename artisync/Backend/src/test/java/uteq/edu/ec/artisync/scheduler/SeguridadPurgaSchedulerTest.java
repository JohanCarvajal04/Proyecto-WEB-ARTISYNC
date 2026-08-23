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
 * Fase 4 mantenimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §7):
 * {@link SeguridadPurgaScheduler} invoca sp_purgar_datos_seguridad (un
 * PROCEDURE con COMMIT interno) vía JdbcTemplate, fuera de cualquier
 * transacción Spring.
 */
@ExtendWith(MockitoExtension.class)
class SeguridadPurgaSchedulerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SeguridadPurgaScheduler scheduler;

    @Test
    void purgarDatosSeguridad_invocaElProcedureConElTamanoDeLoteConfigurado() {
        scheduler.purgarDatosSeguridad();

        verify(jdbcTemplate).update("CALL sp_purgar_datos_seguridad(?)", 1000);
    }

    @Test
    void purgarDatosSeguridad_noPropagaLaExcepcion_siFallaLaConexion() {
        // Best-effort: un fallo de la purga (DB no disponible, timeout) no
        // debe tumbar el hilo del scheduler ni afectar al trafico en vivo. La
        // siguiente ejecucion programada vuelve a intentarlo.
        doThrow(new TransientDataAccessResourceException("simulado: conexión no disponible"))
                .when(jdbcTemplate).update(anyString(), any(Object[].class));

        scheduler.purgarDatosSeguridad();

        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }
}
