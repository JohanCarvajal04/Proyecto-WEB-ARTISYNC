package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long>, JpaSpecificationExecutor<Servicio> {

    @Override
    @EntityGraph(attributePaths = {"perfil", "perfil.usuario", "subcategoria", "subcategoria.categoria"})
    Page<Servicio> findAll(Specification<Servicio> spec, Pageable pageable);

    List<Servicio> findByPerfilIdPerfil(Long idPerfil);

    List<Servicio> findByPerfilIdPerfilAndEstadoPublicacion(Long idPerfil, String estadoPublicacion);

    List<Servicio> findBySubcategoriaIdSubcategoria(Long idSubcategoria);

    @Query("SELECT s FROM Servicio s WHERE " +
           "(:subcategoriaId IS NULL OR s.subcategoria.idSubcategoria = :subcategoriaId) AND " +
           "(:minPrecio IS NULL OR s.precioBase >= :minPrecio) AND " +
           "(:maxPrecio IS NULL OR s.precioBase <= :maxPrecio) AND " +
           "(:query IS NULL OR LOWER(s.tituloServicio) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.descripcionDetallada) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Servicio> buscarServicios(@Param("subcategoriaId") Long subcategoriaId,
                                   @Param("minPrecio") BigDecimal minPrecio,
                                   @Param("maxPrecio") BigDecimal maxPrecio,
                                   @Param("query") String query,
                                   Pageable pageable);
}
