package uteq.edu.ec.artisync.repository.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.CodigoRespaldo2Fa;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodigoRespaldo2FaRepository extends JpaRepository<CodigoRespaldo2Fa, Long> {

    List<CodigoRespaldo2Fa> findByUsuarioIdUsuarioAndUsadoFalse(Long idUsuario);

    void deleteByUsuarioIdUsuario(Long idUsuario);
}
