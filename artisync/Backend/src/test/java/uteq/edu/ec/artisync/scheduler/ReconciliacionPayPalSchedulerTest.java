package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;
import uteq.edu.ec.artisync.repository.legal.PagoGarantiaRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** REQ-NF-019: mismo patron de prueba que VerificacionSchedulerTest. */
@ExtendWith(MockitoExtension.class)
class ReconciliacionPayPalSchedulerTest {

    @Mock private PagoGarantiaRepository pagoGarantiaRepository;
    @Mock private ReconciliacionPayPalEjecutorServicio reconciliacionPayPalEjecutorServicio;

    @InjectMocks
    private ReconciliacionPayPalScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "umbralMinutos", 30);
    }

    @Test
    void reconciliarPagosPendientes_delegaCadaPagoAlEjecutor() {
        PagoGarantia vencido = PagoGarantia.builder().idPago(1L).estadoFondos("Pendiente").build();
        when(pagoGarantiaRepository.findByEstadoFondosAndFechaActualizacionBefore(eq("Pendiente"), any()))
                .thenReturn(List.of(vencido));

        scheduler.reconciliarPagosPendientes();

        verify(reconciliacionPayPalEjecutorServicio).reconciliar(1L);
    }

    @Test
    void reconciliarPagosPendientes_sinPagosVencidos_noHaceNada() {
        when(pagoGarantiaRepository.findByEstadoFondosAndFechaActualizacionBefore(eq("Pendiente"), any()))
                .thenReturn(List.of());

        scheduler.reconciliarPagosPendientes();

        verifyNoInteractions(reconciliacionPayPalEjecutorServicio);
    }

    @Test
    void reconciliarPagosPendientes_unPagoFallaOtroSigueLogueaYContinua() {
        PagoGarantia a = PagoGarantia.builder().idPago(1L).estadoFondos("Pendiente").build();
        PagoGarantia b = PagoGarantia.builder().idPago(2L).estadoFondos("Pendiente").build();
        when(pagoGarantiaRepository.findByEstadoFondosAndFechaActualizacionBefore(eq("Pendiente"), any()))
                .thenReturn(List.of(a, b));
        doThrow(new RuntimeException("fallo simulado")).when(reconciliacionPayPalEjecutorServicio).reconciliar(1L);

        scheduler.reconciliarPagosPendientes();

        // El fallo en 'a' no debe impedir que 'b' se procese: cada uno vive en
        // su propia transaccion (REQUIRES_NEW).
        verify(reconciliacionPayPalEjecutorServicio).reconciliar(1L);
        verify(reconciliacionPayPalEjecutorServicio).reconciliar(2L);
    }
}
