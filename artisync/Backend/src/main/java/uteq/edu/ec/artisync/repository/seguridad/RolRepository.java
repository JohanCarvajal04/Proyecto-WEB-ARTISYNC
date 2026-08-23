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

    /**
     * Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §4) -
     * fn_crear_rol: crea un rol y asigna sus permisos iniciales atomicamente,
     * capturando unique_violation sobre el nombre en vez de una comprobacion
     * findByNombreRol no atomica (A8). Devuelve el id_rol generado.
     */
    @Query(value = "SELECT fn_crear_rol(:p_nombre_rol, :p_descripcion_rol, :p_codigos_permiso)", nativeQuery = true)
    Long crearRol(
            @Param("p_nombre_rol") String nombreRol,
            @Param("p_descripcion_rol") String descripcionRol,
            @Param("p_codigos_permiso") String[] codigosPermiso);
}
