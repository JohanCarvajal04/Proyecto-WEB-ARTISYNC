package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.entity.profile.AiCertificate;
import uteq.edu.ec.artisync.repository.profile.AiCertificateRepository;
import uteq.edu.ec.artisync.service.shared.storage.DocumentStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VerificationExpirationServiceTest {

    @Mock private AiCertificateRepository certificadoIaRepository;
    @Mock private DocumentStorage almacenamiento;

    @InjectMocks
    private VerificationExpirationService servicio;

    @Test
    void expireCertificate_eliminaElDocumentoYMarcaElCertificado() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(1L).urlDocumentoS3("verificacion/doc.pdf").build();

        servicio.expireCertificate(certificado);

        verify(almacenamiento).delete("verificacion/doc.pdf");
        assertThat(certificado.isDocumentoEliminado()).isTrue();
        verify(certificadoIaRepository).save(certificado);
    }
}
