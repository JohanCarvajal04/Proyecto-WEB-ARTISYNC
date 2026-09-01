package uteq.edu.ec.artisync.repository.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CertificadoIaRepository extends JpaRepository<CertificadoIa, Long> {
    boolean existsByUsuarioIdUsuarioAndEstadoVerificacionNombreEstado(Long idUsuario, String nombreEstado);
    List<CertificadoIa> findByUsuarioIdUsuario(Long idUsuario);

    /** Gating de REQ-F-006 ampliado: ¿este usuario tiene su identidad aprobada? */
    boolean existsByUsuarioIdUsuarioAndTipoDocumentoAndEstadoVerificacionNombreEstado(
            Long idUsuario, String tipoDocumento, String nombreEstado);

    /** Última solicitud de identidad de un usuario, para mostrarle su estado actual. */
    java.util.Optional<CertificadoIa> findTopByUsuarioIdUsuarioAndTipoDocumentoOrderByFechaAnalisisDesc(
            Long idUsuario, String tipoDocumento);

    @Query(value = "SELECT * FROM fn_listar_cola_verificacion(:estado, :limite, :offset)", nativeQuery = true)
    List<VerificacionColaProyeccion> listarCola(
            @Param("estado") String estado,
            @Param("limite") int limite,
            @Param("offset") int offset);

    @Procedure(procedureName = "sp_registrar_decision_verificacion")
    void registrarDecision(
            @Param("p_id_certificado") Long idCertificado,
            @Param("p_id_estado") Long idEstado,
            @Param("p_id_moderador") Long idModerador,
            @Param("p_nota") String nota);

    List<CertificadoIa> findByEstadoVerificacionNombreEstadoAndFechaAnalisisBefore(
            String nombreEstado, LocalDateTime limite);
}
