package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;

import java.util.List;

import static org.mockito.Mockito.*;

/** REQ-NF-020: mismo patron de prueba que los demas schedulers de esta ronda. */
@ExtendWith(MockitoExtension.class)
class ContratoIntegridadSchedulerTest {

    @Mock private ContratoRepository contratoRepository;
    @Mock private ContratoIntegridadEjecutorServicio contratoIntegridadEjecutorServicio;

    @InjectMocks
    private ContratoIntegridadScheduler scheduler;

    @Test
    void verificarIntegridadDeTodos_delegaCadaContratoAlEjecutor() {
        Contrato firmado = Contrato.builder().idContrato(1L).hashContenido("abc").build();
        when(contratoRepository.findByHashContenidoIsNotNull()).thenReturn(List.of(firmado));

        scheduler.verificarIntegridadDeTodos();

        verify(contratoIntegridadEjecutorServicio).verificar(1L);
    }

    @Test
    void verificarIntegridadDeTodos_sinContratosFirmados_noHaceNada() {
        when(contratoRepository.findByHashContenidoIsNotNull()).thenReturn(List.of());

        scheduler.verificarIntegridadDeTodos();

        verifyNoInteractions(contratoIntegridadEjecutorServicio);
    }

    @Test
    void verificarIntegridadDeTodos_unContratoFallaOtroSigueLogueaYContinua() {
        Contrato a = Contrato.builder().idContrato(1L).hashContenido("abc").build();
        Contrato b = Contrato.builder().idContrato(2L).hashContenido("def").build();
        when(contratoRepository.findByHashContenidoIsNotNull()).thenReturn(List.of(a, b));
        doThrow(new RuntimeException("fallo simulado")).when(contratoIntegridadEjecutorServicio).verificar(1L);

        scheduler.verificarIntegridadDeTodos();

        verify(contratoIntegridadEjecutorServicio).verificar(1L);
        verify(contratoIntegridadEjecutorServicio).verificar(2L);
    }
}
