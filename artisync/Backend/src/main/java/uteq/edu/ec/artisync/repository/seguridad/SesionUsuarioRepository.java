package uteq.edu.ec.artisync.repository.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.SesionUsuario;

import java.util.List;
import java.util.Optional;

@Repository
public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Long> {

    Optional<SesionUsuario> findByJti(String jti);

    void deleteByJti(String jti);

    List<SesionUsuario> findByUsuarioIdUsuario(Long idUsuario);

    void deleteByUsuarioIdUsuario(Long idUsuario);

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5) -
     * fn_revocar_sesiones_usuario: DELETE ... RETURNING atomico que lee y borra
     * las sesiones del usuario en una sola sentencia y un solo snapshot,
     * eliminando la lectura no repetible que tenia la version en tres pasos
     * (findByUsuarioIdUsuario + revocar en Redis + deleteByUsuarioIdUsuario):
     * una sesion creada entremedias se borraba sin llegar nunca a revocarse.
     */
    @Query(value = "SELECT * FROM fn_revocar_sesiones_usuario(:p_id_usuario)", nativeQuery = true)
    List<SesionRevocadaProyeccion> revocarSesionesUsuario(@Param("p_id_usuario") Long idUsuario);
}
