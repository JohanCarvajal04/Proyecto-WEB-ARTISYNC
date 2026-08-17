package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Solicitudes PENDIENTE sin revisar durante 30 días caducan: se borra el
 * archivo (retención acotada de datos personales) pero el estado sigue
 * PENDIENTE — sigue en la cola, marcada como sin documento disponible.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificacionScheduler {

    private static final int DIAS_EXPIRACION = 30;

    private final CertificadoIaRepository certificadoIaRepository;
    private final AlmacenamientoDocumentos almacenamiento;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void expirarPendientesAntiguas() {
        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_EXPIRACION);
        List<CertificadoIa> vencidas = certificadoIaRepository
                .findByEstadoVerificacionNombreEstadoAndFechaAnalisisBefore("PENDIENTE", limite);

        if (vencidas.isEmpty()) {
            return;
        }

        log.info("[VerificacionScheduler] Expirando {} solicitud(es) PENDIENTE de más de {} días", vencidas.size(), DIAS_EXPIRACION);
        for (CertificadoIa certificado : vencidas) {
            try {
                almacenamiento.eliminar(certificado.getUrlDocumentoS3());
                certificado.setDocumentoEliminado(true);
                certificadoIaRepository.save(certificado);
            } catch (Exception e) {
                log.error("[VerificacionScheduler] Error al expirar verificación {}: {}",
                        certificado.getIdCertificado(), e.getMessage(), e);
            }
        }
    }
}
