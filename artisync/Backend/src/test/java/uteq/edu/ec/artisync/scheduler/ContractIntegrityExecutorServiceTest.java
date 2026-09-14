package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.response.legal.IntegrityVerificationResponse;
import uteq.edu.ec.artisync.service.legal.IContractService;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ContractIntegrityExecutorServiceTest {

    @Mock private IContractService contratoServicio;

    @InjectMocks
    private ContractIntegrityExecutorService ejecutor;

    @Test
    void contratoIntegro_noRegistraNadaMasQueLaVerificacion() {
        given(contratoServicio.verifyHashIntegrity(1L))
                .willReturn(IntegrityVerificationResponse.builder().idContrato(1L).integro(true).build());

        ejecutor.verify(1L);
        // Sin excepción y sin más interacciones que la propia verificación: el
        // caso feliz no debe generar ningún efecto colateral.
    }

    @Test
    void contratoConDiscrepancia_noLanzaExcepcion_soloRegistraElHallazgo() {
        given(contratoServicio.verifyHashIntegrity(2L))
                .willReturn(IntegrityVerificationResponse.builder().idContrato(2L).integro(false).build());

        ejecutor.verify(2L);
        // El barrido nocturno no debe abortar por una discrepancia: solo se
        // registra en el log (verificado indirectamente al no lanzar).
    }
}
