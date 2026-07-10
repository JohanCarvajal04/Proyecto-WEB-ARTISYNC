package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.Subcategoria;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Long> {

    List<Subcategoria> findByCategoriaIdCategoriaOrderByNombreSubcategoriaAsc(Long idCategoria);

    List<Subcategoria> findAllByOrderByNombreSubcategoriaAsc();

    Optional<Subcategoria> findByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(Long idCategoria, String nombreSubcategoria);

    boolean existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(Long idCategoria, String nombreSubcategoria);
}
