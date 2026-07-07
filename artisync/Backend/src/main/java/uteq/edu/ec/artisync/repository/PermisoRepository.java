package uteq.edu.ec.artisync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.Permiso;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    Optional<Permiso> findByNombrePermiso(String nombrePermiso);

    List<Permiso> findByModuloAplicacion(String moduloAplicacion);
}
