package uteq.edu.ec.artisync.repository;

import uteq.edu.ec.artisync.model.seguridad.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PermisoRepository extends JpaRepository<Permiso, Integer> {
    List<Permiso> findByModuloAplicacion(String modulo);
}
