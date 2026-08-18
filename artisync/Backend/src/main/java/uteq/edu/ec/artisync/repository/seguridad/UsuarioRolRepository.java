package uteq.edu.ec.artisync.repository.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;

import java.util.List;

@Repository
public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {

    List<UsuarioRol> findByUsuarioIdUsuario(Long idUsuario);

    List<UsuarioRol> findByUsuarioCorreo(String correo);

    boolean existsByRolIdRol(Long idRol);

    /**
     * Ids de los usuarios que tienen un rol dado. Se usa al sincronizar los
     * permisos de un rol para revocar sus sesiones: los permisos viajan en el
     * claim `permisos` del JWT, así que sin revocar seguirían operando con los
     * permisos antiguos hasta que el token caducara.
     */
    @Query("SELECT ur.usuario.idUsuario FROM UsuarioRol ur WHERE UPPER(ur.rol.nombreRol) = UPPER(:nombreRol)")
    List<Long> findIdsUsuarioByNombreRol(@Param("nombreRol") String nombreRol);
}
