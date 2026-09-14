package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.entity.legal.Contract;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;

import java.util.List;

import static org.mockito.Mockito.*;

/** REQ-NF-020: mismo patron de prueba que los demas schedulers de esta ronda. */
@ExtendWith(MockitoExtension.class)
class ContractIntegritySchedulerTest {

    @Mock private ContractRepository contratoRepository;
    @Mock private ContractIntegrityExecutorService contratoIntegridadEjecutorServicio;

    @InjectMocks
    private ContractIntegrityScheduler scheduler;

    @Test
    void verificarIntegridadDeTodos_delegaCadaContratoAlEjecutor() {
        Contract firmado = Contract.builder().idContrato(1L).hashContenido("abc").build();
        when(contratoRepository.findByHashContenidoIsNotNull()).thenReturn(List.of(firmado));

        scheduler.verifyIntegrityOfAll();

        verify(contratoIntegridadEjecutorServicio).verify(1L);
    }

    @Test
    void verificarIntegridadDeTodos_sinContratosFirmados_noHaceNada() {
        when(contratoRepository.findByHashContenidoIsNotNull()).thenReturn(List.of());

        scheduler.verifyIntegrityOfAll();

        verifyNoInteractions(contratoIntegridadEjecutorServicio);
    }

    @Test
    void verificarIntegridadDeTodos_unContratoFallaOtroSigueLogueaYContinua() {
        Contract a = Contract.builder().idContrato(1L).hashContenido("abc").build();
        Contract b = Contract.builder().idContrato(2L).hashContenido("def").build();
        when(contratoRepository.findByHashContenidoIsNotNull()).thenReturn(List.of(a, b));
        doThrow(new RuntimeException("fallo simulado")).when(contratoIntegridadEjecutorServicio).verify(1L);

        scheduler.verifyIntegrityOfAll();

        verify(contratoIntegridadEjecutorServicio).verify(1L);
        verify(contratoIntegridadEjecutorServicio).verify(2L);
    }
}
