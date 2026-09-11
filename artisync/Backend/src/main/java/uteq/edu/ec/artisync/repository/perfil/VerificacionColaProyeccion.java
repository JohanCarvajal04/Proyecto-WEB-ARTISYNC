package uteq.edu.ec.artisync.repository.perfil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link VerificacionColaProyeccion}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
public interface VerificacionColaProyeccion {
    Long getIdCertificado();
    Long getIdUsuario();
    String getNombreUsuario();
    String getTipoDocumento();
    String getNombreEstado();
    String getVeredictoIa();
    BigDecimal getPuntajeConfianzaIa();
    LocalDateTime getFechaAnalisis();
}

