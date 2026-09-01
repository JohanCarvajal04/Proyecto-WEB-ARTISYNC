package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;

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
    private final VerificacionExpiracionServicio verificacionExpiracionServicio;

    /**
     * Sin @Transactional aquí: si todo el lote compartiera una única
     * transacción/conexión, una excepción en un certificado la dejaría
     * "aborted" en Postgres hasta el COMMIT final -- el try/catch por
     * elemento no la libera, así que los certificados siguientes fallarían
     * en cascada y, al no poder confirmar el método, Spring revertiría
     * también los ya procesados con éxito. Cada certificado se expira en su
     * propia transacción (VerificacionExpiracionServicio.expirarCertificado,
     * REQUIRES_NEW).
     */
    @Scheduled(cron = "0 0 3 * * *")
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
                verificacionExpiracionServicio.expirarCertificado(certificado);
            } catch (Exception e) {
                log.error("[VerificacionScheduler] Error al expirar verificación {}: {}",
                        certificado.getIdCertificado(), e.getMessage(), e);
            }
        }
    }
}
