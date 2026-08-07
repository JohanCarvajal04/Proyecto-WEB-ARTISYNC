package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificacionSchedulerTest {

    @Mock private CertificadoIaRepository certificadoIaRepository;
    @Mock private AlmacenamientoDocumentos almacenamiento;

    @InjectMocks
    private VerificacionScheduler scheduler;

    @Test
    void expirarPendientesAntiguas_borraElArchivoYMarcaDocumentoEliminado() {
        CertificadoIa vencida = CertificadoIa.builder()
                .idCertificado(1L).urlDocumentoS3("ref.jpg").documentoEliminado(false).build();
        when(certificadoIaRepository.findByEstadoVerificacionNombreEstadoAndFechaAnalisisBefore(eq("PENDIENTE"), any()))
                .thenReturn(List.of(vencida));
        when(certificadoIaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.expirarPendientesAntiguas();

        verify(almacenamiento).eliminar("ref.jpg");
        verify(certificadoIaRepository).save(argThat(CertificadoIa::isDocumentoEliminado));
    }

    @Test
    void expirarPendientesAntiguas_sinPendientesVencidas_noHaceNada() {
        when(certificadoIaRepository.findByEstadoVerificacionNombreEstadoAndFechaAnalisisBefore(eq("PENDIENTE"), any()))
                .thenReturn(List.of());

        scheduler.expirarPendientesAntiguas();

        verifyNoInteractions(almacenamiento);
        verify(certificadoIaRepository, never()).save(any());
    }
}
