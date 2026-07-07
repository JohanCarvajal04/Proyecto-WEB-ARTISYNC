package uteq.edu.ec.artisync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.RolPermiso;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolPermisoRepository extends JpaRepository<RolPermiso, Long> {

    List<RolPermiso> findByRolIdRol(Long idRol);

    Optional<RolPermiso> findByRolIdRolAndPermisoIdPermiso(Long idRol, Long idPermiso);

    void deleteByRolIdRol(Long idRol);
}
