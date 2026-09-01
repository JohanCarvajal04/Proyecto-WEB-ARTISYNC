package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificacionSchedulerTest {

    @Mock private CertificadoIaRepository certificadoIaRepository;
    @Mock private VerificacionExpiracionServicio verificacionExpiracionServicio;

    @InjectMocks
    private VerificacionScheduler scheduler;

    @Test
    void expirarPendientesAntiguas_delegaCadaCertificadoAlServicioDeExpiracion() {
        CertificadoIa vencida = CertificadoIa.builder()
                .idCertificado(1L).urlDocumentoS3("ref.jpg").documentoEliminado(false).build();
        when(certificadoIaRepository.findByEstadoVerificacionNombreEstadoAndFechaAnalisisBefore(eq("PENDIENTE"), any()))
                .thenReturn(List.of(vencida));

        scheduler.expirarPendientesAntiguas();

        verify(verificacionExpiracionServicio).expirarCertificado(vencida);
    }

    @Test
    void expirarPendientesAntiguas_sinPendientesVencidas_noHaceNada() {
        when(certificadoIaRepository.findByEstadoVerificacionNombreEstadoAndFechaAnalisisBefore(eq("PENDIENTE"), any()))
                .thenReturn(List.of());

        scheduler.expirarPendientesAntiguas();

        verifyNoInteractions(verificacionExpiracionServicio);
    }

    @Test
    void expirarPendientesAntiguas_unCertificadoFallaOtroSiguelogueaYContinua() {
        CertificadoIa a = CertificadoIa.builder().idCertificado(1L).urlDocumentoS3("a.jpg").build();
        CertificadoIa b = CertificadoIa.builder().idCertificado(2L).urlDocumentoS3("b.jpg").build();
        when(certificadoIaRepository.findByEstadoVerificacionNombreEstadoAndFechaAnalisisBefore(eq("PENDIENTE"), any()))
                .thenReturn(List.of(a, b));
        doThrow(new RuntimeException("fallo simulado")).when(verificacionExpiracionServicio).expirarCertificado(a);

        scheduler.expirarPendientesAntiguas();

        // El fallo en 'a' no debe impedir que 'b' se procese: cada uno vive
        // en su propia transacción (REQUIRES_NEW), así que un error aislado
        // no debe abortar el resto del lote.
        verify(verificacionExpiracionServicio).expirarCertificado(a);
        verify(verificacionExpiracionServicio).expirarCertificado(b);
    }
}
