package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.ServicioAtributo;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicioAtributoRepository extends JpaRepository<ServicioAtributo, Long> {

    long countByServicioIdServicio(Long idServicio);

    List<ServicioAtributo> findByServicioIdServicio(Long idServicio);

    Optional<ServicioAtributo> findByServicioIdServicioAndAtributoIdAtributo(Long idServicio, Long idAtributo);

    void deleteByServicioIdServicio(Long idServicio);
}
