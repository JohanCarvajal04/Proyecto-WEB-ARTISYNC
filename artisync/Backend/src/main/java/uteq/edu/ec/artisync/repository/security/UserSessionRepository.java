package uteq.edu.ec.artisync.repository.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.security.UserSession;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link UserSession}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long>, UserSessionRepositoryCustom {

    /** Sesión activa por su JTI. */
    Optional<UserSession> findByJti(String jti);

    /** Elimina una sesión por su JTI. */
    void deleteByJti(String jti);

    // CR-02 (revision de codigo): findByUsuarioIdUsuario y
    // deleteByUsuarioIdUsuario vivian aqui hasta el refactor de concurrencia.
    // Eran el patron en tres pasos (leer sesiones -> revocar en Redis -> borrar)
    // que revocarSesionesUsuario/fn_revocar_sesiones_usuario sustituyo: una
    // sesion creada entre el primer y el ultimo paso se borraba sin haberse
    // revocado nunca en Redis (lectura no repetible, A6). Se eliminan -no se
    // dejan como codigo muerto- para que nadie los reintroduzca sin darse
    // cuenta de que reabren esa anomalia.
}


