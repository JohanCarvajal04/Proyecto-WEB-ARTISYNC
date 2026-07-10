package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.ServicioEtiqueta;

import java.util.List;

@Repository
public interface ServicioEtiquetaRepository extends JpaRepository<ServicioEtiqueta, Long> {

    List<ServicioEtiqueta> findByServicioIdServicio(Long idServicio);

    void deleteByServicioIdServicio(Long idServicio);
}
