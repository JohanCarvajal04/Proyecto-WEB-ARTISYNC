package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;

/**
 * Extraído de VerificacionScheduler para que REQUIRES_NEW funcione de
 * verdad: un método @Transactional llamado desde dentro de la misma clase
 * (this.metodo()) se salta el proxy de Spring AOP, así que la anotación se
 * ignoraría en silencio. Al vivir en un bean distinto, VerificacionScheduler
 * lo invoca a través del proxy real y cada certificado queda en su propia
 * transacción.
 */
@Component
@RequiredArgsConstructor
public class VerificacionExpiracionServicio {

    private final CertificadoIaRepository certificadoIaRepository;
    private final AlmacenamientoDocumentos almacenamiento;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expirarCertificado(CertificadoIa certificado) {
        almacenamiento.eliminar(certificado.getUrlDocumentoS3());
        certificado.setDocumentoEliminado(true);
        certificadoIaRepository.save(certificado);
    }
}
