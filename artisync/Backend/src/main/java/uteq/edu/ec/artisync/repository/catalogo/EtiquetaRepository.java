package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.Etiqueta;

import java.util.List;
import java.util.Optional;

@Repository
public interface EtiquetaRepository extends JpaRepository<Etiqueta, Long> {

    Optional<Etiqueta> findByNombreEtiquetaIgnoreCase(String nombreEtiqueta);

    List<Etiqueta> findByNombreEtiquetaIn(List<String> nombres);

    boolean existsByNombreEtiquetaIgnoreCase(String nombreEtiqueta);
}
