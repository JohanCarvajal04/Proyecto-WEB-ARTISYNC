package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.entity.social.Raffle;
import uteq.edu.ec.artisync.repository.social.RaffleRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RaffleSchedulerTest {

    @Mock private RaffleRepository sorteoRepository;
    @Mock private RaffleExecutorService sorteoEjecutorServicio;

    @InjectMocks
    private RaffleScheduler scheduler;

    @Test
    void sinSorteosPendientes_noLlamaAlEjecutor() {
        given(sorteoRepository.findByEstadoSorteoAndFechaCierreBefore(anyString(), any()))
                .willReturn(List.of());

        scheduler.processClosedRaffles();

        verify(sorteoEjecutorServicio, times(0)).executeRaffle(any());
    }

    @Test
    void conSorteosPendientes_ejecutaCadaUnoEnSuPropiaTransaccion() {
        Raffle sorteo1 = Raffle.builder().idSorteo(1L).build();
        Raffle sorteo2 = Raffle.builder().idSorteo(2L).build();
        given(sorteoRepository.findByEstadoSorteoAndFechaCierreBefore(anyString(), any()))
                .willReturn(List.of(sorteo1, sorteo2));

        scheduler.processClosedRaffles();

        verify(sorteoEjecutorServicio).executeRaffle(sorteo1);
        verify(sorteoEjecutorServicio).executeRaffle(sorteo2);
    }

    @Test
    void unSorteoFallaConExcepcion_losSiguientesSeSiguenProcesando() {
        Raffle sorteoConError = Raffle.builder().idSorteo(1L).build();
        Raffle sorteoOk = Raffle.builder().idSorteo(2L).build();
        given(sorteoRepository.findByEstadoSorteoAndFechaCierreBefore(anyString(), any()))
                .willReturn(List.of(sorteoConError, sorteoOk));
        doThrow(new RuntimeException("fallo simulado")).when(sorteoEjecutorServicio).executeRaffle(sorteoConError);

        scheduler.processClosedRaffles();

        verify(sorteoEjecutorServicio).executeRaffle(sorteoOk);
    }
}
