package uteq.edu.ec.artisync.repository.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.CodigoRespaldo2Fa;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link CodigoRespaldo2Fa}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface CodigoRespaldo2FaRepository extends JpaRepository<CodigoRespaldo2Fa, Long> {

    // CR-02 (revision de codigo): findByUsuarioIdUsuarioAndUsadoFalse y
    // deleteByUsuarioIdUsuario vivian aqui hasta el refactor de concurrencia.
    // Eran, respectivamente, la mitad del patron read-modify-write que
    // permitia consumir dos veces el mismo codigo de respaldo (bypass de 2FA,
    // A1) y el borrado sin revocacion previa (A6) que sustituyo
    // consumirCodigoRespaldo/fn_consumir_codigo_respaldo_2fa mas abajo. Se
    // eliminan -no se dejan como codigo muerto- para que nadie los reintroduzca
    // sin darse cuenta de que reabren esas dos anomalias.

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md Â§2) -
     * fn_consumir_codigo_respaldo_2fa: UPDATE atomico ({@code WHERE usado = FALSE})
     * que consume un codigo de respaldo una sola vez, eliminando la actualizacion
     * perdida del patron anterior (SELECT de todos los codigos + comparacion en
     * Java + save()). Devuelve TRUE solo para el primer llamante concurrente.
     *
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * @Procedure con retorno no-void rompe con Hibernate 7.4.1 contra una FUNCTION de Postgres
     * (genera sintaxis de argumento nombrado "p_x => ?" dentro del escape JDBC, invalida). Ver el
     * hallazgo completo en docs/basedatos/CATALOGO-SP.md ??14.
     */
    @Query(value = "SELECT fn_consumir_codigo_respaldo_2fa(:p_id_usuario, :p_codigo_hash)", nativeQuery = true)
    Boolean consumirCodigoRespaldo(
            @Param("p_id_usuario") Long idUsuario,
            @Param("p_codigo_hash") String codigoHash);
}


