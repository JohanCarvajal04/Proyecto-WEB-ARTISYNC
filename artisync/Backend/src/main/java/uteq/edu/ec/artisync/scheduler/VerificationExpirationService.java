package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.entity.perfil.AiCertificate;
import uteq.edu.ec.artisync.repository.perfil.AiCertificateRepository;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;

/**
 * Extraído de VerificationScheduler para que REQUIRES_NEW funcione de
 * verdad: un método @Transactional llamado desde dentro de la misma clase
 * (this.metodo()) se salta el proxy de Spring AOP, así que la anotación se
 * ignoraría en silencio. Al vivir en un bean distinto, VerificationScheduler
 * lo invoca a través del proxy real y cada certificado queda en su propia
 * transacción.
 */
@Component
@RequiredArgsConstructor
public class VerificationExpirationService {

    private final AiCertificateRepository certificadoIaRepository;
    private final DocumentStorage almacenamiento;

    /**
     * Elimina el documento de un certificado de IA vencido del almacenamiento
     * y marca la entidad como {@code documentoEliminado}.
     *
     * @param certificado certificado cuyo documento ya venció
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expirarCertificado(AiCertificate certificado) {
        almacenamiento.eliminar(certificado.getUrlDocumentoS3());
        certificado.setDocumentoEliminado(true);
        certificadoIaRepository.save(certificado);
    }
}
