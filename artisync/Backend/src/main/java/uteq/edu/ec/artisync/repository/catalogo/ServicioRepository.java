package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long>, JpaSpecificationExecutor<Servicio> {

    @Override
    @EntityGraph(attributePaths = {"perfil", "perfil.usuario"})
    Page<Servicio> findAll(Specification<Servicio> spec, Pageable pageable);

    List<Servicio> findByPerfilIdPerfil(Long idPerfil);

    List<Servicio> findByPerfilIdPerfilAndEstadoPublicacion(Long idPerfil, String estadoPublicacion);
}
