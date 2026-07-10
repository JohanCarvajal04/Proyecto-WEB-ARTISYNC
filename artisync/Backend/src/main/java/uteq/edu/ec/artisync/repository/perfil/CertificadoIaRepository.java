package uteq.edu.ec.artisync.repository.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;

import java.util.List;

@Repository
public interface CertificadoIaRepository extends JpaRepository<CertificadoIa, Long> {
    boolean existsByPerfilIdPerfilAndEstadoVerificacionNombreEstado(Long idPerfil, String nombreEstado);
    List<CertificadoIa> findByPerfilIdPerfil(Long idPerfil);
}
