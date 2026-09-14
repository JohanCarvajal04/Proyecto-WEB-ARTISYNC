package uteq.edu.ec.artisync.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.entity.security.User;
import uteq.edu.ec.artisync.entity.social.Raffle;
import uteq.edu.ec.artisync.repository.security.UserRepository;
import uteq.edu.ec.artisync.repository.social.RaffleRepository;
import uteq.edu.ec.artisync.service.communication.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RaffleExecutorServiceTest {

    @Mock private RaffleRepository sorteoRepository;
    @Mock private UserRepository usuarioRepository;
    @Mock private NotificationService notificacionService;

    // ObjectMapper real (no mock): el ejecutor solo lo usa para parsear el
    // JSONB que ya viene serializado como texto desde fn_seleccionar_ganadores_sorteo.
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RaffleExecutorService conObjectMapperReal() {
        return new RaffleExecutorService(sorteoRepository, usuarioRepository, notificacionService, objectMapper);
    }

    @Test
    void sorteoConGanadores_notificaACadaGanador() {
        RaffleExecutorService ejecutor = conObjectMapperReal();
        Raffle sorteo = Raffle.builder().idSorteo(1L).tituloSorteo("Sorteo de prueba").build();
        given(sorteoRepository.seleccionarGanadores(1L)).willReturn("""
                {"estado":"Finalizado","tituloSorteo":"Sorteo de prueba",
                 "ganadores":[{"idUsuario":10,"descripcionPremio":"Ilustración"},{"idUsuario":20}]}""");
        User ganador1 = User.builder().idUsuario(10L).build();
        User ganador2 = User.builder().idUsuario(20L).build();
        given(usuarioRepository.getReferenceById(10L)).willReturn(ganador1);
        given(usuarioRepository.getReferenceById(20L)).willReturn(ganador2);

        ejecutor.executeRaffle(sorteo);

        verify(notificacionService).notify(org.mockito.ArgumentMatchers.eq(ganador1), anyString(),
                org.mockito.ArgumentMatchers.contains("Ilustración"));
        verify(notificacionService).notify(org.mockito.ArgumentMatchers.eq(ganador2), anyString(),
                org.mockito.ArgumentMatchers.contains("Sorteo de prueba"));
    }

    @Test
    void sorteoSinGanadores_noNotificaANadie() {
        RaffleExecutorService ejecutor = conObjectMapperReal();
        Raffle sorteo = Raffle.builder().idSorteo(2L).tituloSorteo("Sin participantes").build();
        given(sorteoRepository.seleccionarGanadores(2L)).willReturn("""
                {"estado":"Sin_Participantes","ganadores":[]}""");

        ejecutor.executeRaffle(sorteo);

        verifyNoInteractions(notificacionService);
        verify(usuarioRepository, never()).getReferenceById(any());
    }

    @Test
    void resultadoJsonInvalido_lanzaIllegalStateException() {
        RaffleExecutorService ejecutor = conObjectMapperReal();
        Raffle sorteo = Raffle.builder().idSorteo(3L).build();
        given(sorteoRepository.seleccionarGanadores(3L)).willReturn("esto no es JSON");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> ejecutor.executeRaffle(sorteo));
    }
}
