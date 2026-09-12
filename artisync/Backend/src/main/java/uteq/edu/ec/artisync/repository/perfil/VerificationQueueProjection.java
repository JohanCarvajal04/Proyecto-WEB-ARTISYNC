package uteq.edu.ec.artisync.repository.perfil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proyección de Spring Data para una fila de la cola de verificación de
 * identidad (certificados de IA pendientes de revisión por un moderador).
 */
public interface VerificationQueueProjection {
    /** @return identificador del certificado de IA */
    Long getIdCertificado();
    /** @return identificador del usuario que solicitó la verificación */
    Long getIdUsuario();
    /** @return nombre completo del usuario */
    String getNombreUsuario();
    /** @return tipo de documento presentado */
    String getTipoDocumento();
    /** @return nombre del estado de verificación actual */
    String getNombreEstado();
    /** @return veredicto emitido por el análisis de IA */
    String getVeredictoIa();
    /** @return puntaje de confianza del análisis de IA */
    BigDecimal getPuntajeConfianzaIa();
    /** @return fecha en que se realizó el análisis de IA */
    LocalDateTime getFechaAnalisis();
}

