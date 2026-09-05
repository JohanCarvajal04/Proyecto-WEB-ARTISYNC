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

    // CR-02 (revision de codigo): findByUsuarioIdUsuario y
    // deleteByUsuarioIdUsuario vivian aqui hasta el refactor de concurrencia.
    // Eran el patron en tres pasos (leer sesiones -> revocar en Redis -> borrar)
    // que revocarSesionesUsuario/fn_revocar_sesiones_usuario sustituyo: una
    // sesion creada entre el primer y el ultimo paso se borraba sin haberse
    // revocado nunca en Redis (lectura no repetible, A6). Se eliminan -no se
    // dejan como codigo muerto- para que nadie los reintroduzca sin darse
    // cuenta de que reabren esa anomalia.

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md ??5) -
     * fn_revocar_sesiones_usuario: DELETE ... RETURNING atomico que lee y borra
     * las sesiones del usuario en una sola sentencia y un solo snapshot,
     * eliminando la lectura no repetible que tenia la version en tres pasos
     * (findByUsuarioIdUsuario + revocar en Redis + deleteByUsuarioIdUsuario):
     * una sesion creada entremedias se borraba sin llegar nunca a revocarse.
     */
        /**
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery]
     * Esta rutina devuelve un result set (TABLE) complejo proyectado en una interfaz Spring Data (DTO).
     * El mecanismo @Procedure (o @NamedStoredProcedureQuery) en PostgreSQL exige la devolucion de un RefCursor
     * como parametro OUT para mapear tablas, lo que colisiona con el soporte nativo de Proyecciones de Hibernate.
     * Por lo tanto, para funciones que devuelven multiples columnas como filas, nativeQuery=true es el mecanismo
     * recomendado y correcto que evita acoplar el esquema de BD a DTOs de mapeo hiper-estrictos.
     */
    @Query(value = "SELECT * FROM fn_revocar_sesiones_usuario(:p_id_usuario)", nativeQuery = true)
    List<SesionRevocadaProyeccion> revocarSesionesUsuario(@Param("p_id_usuario") Long idUsuario);
}

