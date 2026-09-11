package uteq.edu.ec.artisync.repository.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.AutenticacionDosFactores;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link AutenticacionDosFactores}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface AutenticacionDosFactoresRepository extends JpaRepository<AutenticacionDosFactores, Long> {

    Optional<AutenticacionDosFactores> findByUsuarioIdUsuario(Long idUsuario);

    Optional<AutenticacionDosFactores> findByUsuarioCorreo(String correo);

    /**
     * Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md Â§8) - carga en
     * UNA sola consulta el estado de 2FA de TODOS los usuarios de
     * {@code idsUsuario}. La usa UsuarioMapper.toUserResponseList para eliminar
     * el N+1 de invocar findByUsuarioIdUsuario por cada fila de una pagina de
     * administracion de usuarios.
     */
    List<AutenticacionDosFactores> findByUsuarioIdUsuarioIn(List<Long> idsUsuario);

    /**
     * Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md Â§7) -
     * fn_configurar_2fa: upsert atomico del secreto TOTP + reemplazo completo
     * de codigos de respaldo en una unica transaccion. Sustituye la escritura
     * en 10 pasos de TwoFactorServiceImpl.setup2Fa (A4). Devuelve el numero de
     * codigos de respaldo insertados.
     *
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * @Procedure con retorno no-void rompe con Hibernate 7.4.1 contra una FUNCTION de Postgres
     * (genera sintaxis de argumento nombrado "p_x => ?" dentro del escape JDBC, invalida). Ver el
     * hallazgo completo en docs/basedatos/CATALOGO-SP.md ??14.
     */
    @Query(value = "SELECT fn_configurar_2fa(:p_id_usuario, :p_llave_secreta, :p_hashes)", nativeQuery = true)
    Integer configurar2Fa(
            @Param("p_id_usuario") Long idUsuario,
            @Param("p_llave_secreta") String llaveSecreta,
            @Param("p_hashes") String[] hashes);

    /**
     * Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md Â§7) -
     * fn_desactivar_2fa: desactiva 2FA y purga codigos de respaldo
     * atomicamente; idempotente (FALSE, no excepcion) si el usuario no tenia
     * 2FA configurado. Unifica el codigo antes duplicado entre
     * TwoFactorServiceImpl.disable2Fa y AdminUserServiceImpl.updateUser (A4).
     */
    @Query(value = "SELECT fn_desactivar_2fa(:p_id_usuario)", nativeQuery = true)
    Boolean desactivar2Fa(@Param("p_id_usuario") Long idUsuario);
}


