package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.Categoria;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByEstadoActivaTrueOrderByNombreCategoriaAsc();

    List<Categoria> findAllByOrderByNombreCategoriaAsc();

    Optional<Categoria> findByNombreCategoriaIgnoreCase(String nombreCategoria);

    boolean existsByNombreCategoriaIgnoreCase(String nombreCategoria);
}
