package uteq.edu.ec.artisync.repository.perfil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Proyección de fn_listar_cola_verificacion; Spring Data mapea snake_case -> getters. */
public interface VerificacionColaProyeccion {
    Long getIdCertificado();
    Long getIdPerfil();
    String getNombreCreador();
    String getTipoDocumento();
    String getNombreEstado();
    String getVeredictoIa();
    BigDecimal getPuntajeConfianzaIa();
    LocalDateTime getFechaAnalisis();
}
