package uteq.edu.ec.artisync.repository.security;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.security.User;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link User}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>, UserRepositoryCustom {

    /**
     * Usuario por correo exacto; base del login y de la resolución de identidad.
     * @param correo el correo
     * @return un Optional con User si existe, vacio en caso contrario
     */
    Optional<User> findByCorreo(String correo);

    /**
     * @param correo el correo
     * @return {@code true} si ya existe un usuario con ese correo
     */
    boolean existsByCorreo(String correo);

    /**
     * Usuario por id, solo si su cuenta sigue habilitada.
     * @param idUsuario el identificador de usuario
     * @return un Optional con User si existe, vacio en caso contrario
     */
    Optional<User> findByIdUsuarioAndEstadoCuentaTrue(Long idUsuario);

    /**
     * @param idPais el identificador de pais
     * @return {@code true} si algún usuario está registrado con ese país (bloquea su eliminación)
     */
    boolean existsByPaisIdPais(Long idPais);

    /**
     * REQ-NF-018: igual que findById, pero con bloqueo pesimista de fila
     * (mismo patrón que ContractRepository.findByIdParaFirmar). Sin esto, dos
     * solicitudes de supresión casi simultáneas para el mismo usuario (doble
     * clic, autoservicio + admin a la vez) podían pasar ambas el chequeo de
     * "¿ya está anonimizado?" antes de que la primera confirmara su cambio,
     * ejecutando la anonimización dos veces. El bloqueo serializa las dos
     * transacciones: la segunda espera a que la primera confirme y entonces
     * relee el correo ya anonimizado, evitando la repetición.
     * @param idUsuario el identificador de usuario
     * @return un Optional con User si existe, vacio en caso contrario
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.idUsuario = :idUsuario")
    Optional<User> findByIdParaAnonimizar(@Param("idUsuario") Long idUsuario);

    /**
     * REQ-F-005 - sp_restablecer_contrasena: valida token de recuperacion y
     * actualiza el hash de contrasena. PROCEDURE real (no FUNCTION): con
     * Hibernate 7, @Procedure + @Param nombrados contra una FUNCTION escalar
     * genera una llamada con sintaxis "p_x => ?" que Postgres no puede
     * parsear dentro del escape JDBC {call ...}. El caller ya descartaba el
     * valor de retorno (exito = no lanzo excepcion), asi que no hace falta
     * ningun parametro OUT -- a diferencia de las rutinas movidas a
     * {@link UserRepositoryCustom} (que SI necesitan devolver un valor), este
     * caso calza con el unico patron de @Procedure verificado en el proyecto
     * (sp_registrar_decision_verificacion): solo parametros IN, metodo void.
     * @param hashToken el hash token
     * @param nuevaContrasenaHash el nueva contrasena hash
     */
    @Procedure(procedureName = "sp_restablecer_contrasena")
    void restablecerContrasena(
            @Param("p_hash_token") String hashToken,
            @Param("p_nueva_contrasena_hash") String nuevaContrasenaHash);

    /**
     * Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §6) -
     * sp_cambiar_contrasena: UPDATE condicionado (compare-and-swap sobre el
     * hash) que aplica el cambio solo si nadie mas la cambio primero, cerrando
     * la actualizacion perdida (A7); lanza excepcion (ERRCODE 40001) si el
     * hash ya no coincidia. PROCEDURE real (no FUNCTION), mismo motivo que
     * sp_restablecer_contrasena de arriba: el caller ya descartaba el
     * booleano de retorno, asi que no hace falta ningun parametro OUT.
     * @param idUsuario el identificador de usuario
     * @param hashEsperado el hash esperado
     * @param hashNuevo el hash nuevo
     */
    @Procedure(procedureName = "sp_cambiar_contrasena")
    void cambiarContrasena(
            @Param("p_id_usuario") Long idUsuario,
            @Param("p_hash_esperado") String hashEsperado,
            @Param("p_hash_nuevo") String hashNuevo);
}
