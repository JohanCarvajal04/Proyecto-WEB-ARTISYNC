package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.ServicioSubcategoria;

import java.util.List;

@Repository
public interface ServicioSubcategoriaRepository extends JpaRepository<ServicioSubcategoria, Long> {

    List<ServicioSubcategoria> findByServicioIdServicio(Long idServicio);

    List<ServicioSubcategoria> findByServicioIdServicioIn(List<Long> idsServicio);

    void deleteByServicioIdServicio(Long idServicio);

    void deleteByServicioIdServicioAndSubcategoriaIdSubcategoria(Long idServicio, Long idSubcategoria);

    long countByServicioIdServicio(Long idServicio);

    boolean existsBySubcategoriaIdSubcategoria(Long idSubcategoria);

    boolean existsBySubcategoriaCategoriaIdCategoria(Long idCategoria);
}
