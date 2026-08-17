package uteq.edu.ec.artisync.repository.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.Rol;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombreRol(String nombreRol);

    /** REQ-F-003 - fn_sincronizar_permisos_rol: reemplaza atomicamente el set de permisos de un rol. Devuelve el total asignado. */
    @Query(value = "SELECT fn_sincronizar_permisos_rol(:p_nombre_rol, :p_codigos_permiso)", nativeQuery = true)
    Integer sincronizarPermisos(
            @Param("p_nombre_rol") String nombreRol,
            @Param("p_codigos_permiso") String[] codigosPermiso);

    /** REQ-F-004 - fn_eliminar_rol: elimina un rol solo si no es protegido y no tiene usuarios asignados. */
    @Query(value = "SELECT fn_eliminar_rol(:p_id_rol)", nativeQuery = true)
    Boolean eliminarRol(@Param("p_id_rol") Long idRol);
}
