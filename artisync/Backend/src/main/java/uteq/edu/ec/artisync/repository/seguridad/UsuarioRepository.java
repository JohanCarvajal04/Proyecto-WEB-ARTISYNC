package uteq.edu.ec.artisync.repository.seguridad;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByIdUsuarioAndEstadoCuentaTrue(Long idUsuario);

    boolean existsByPaisIdPais(Long idPais);

    /**
     * REQ-NF-018: igual que findById, pero con bloqueo pesimista de fila
     * (mismo patrón que ContratoRepository.findByIdParaFirmar). Sin esto, dos
     * solicitudes de supresión casi simultáneas para el mismo usuario (doble
     * clic, autoservicio + admin a la vez) podían pasar ambas el chequeo de
     * "¿ya está anonimizado?" antes de que la primera confirmara su cambio,
     * ejecutando la anonimización dos veces. El bloqueo serializa las dos
     * transacciones: la segunda espera a que la primera confirme y entonces
     * relee el correo ya anonimizado, evitando la repetición.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.idUsuario = :idUsuario")
    Optional<Usuario> findByIdParaAnonimizar(@Param("idUsuario") Long idUsuario);

    /**
     * REQ-F-001 - fn_registrar_usuario: inserta usuario + usuario_roles +
     * perfil de creador opcional. Devuelve el id_usuario generado.
     *
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * Ver el comentario completo en permisosEfectivos: con Hibernate 7.4.1,
     * @Procedure con un tipo de retorno no-void (que exige un parametro OUT
     * en la PROCEDURE) genera una llamada invalida con sintaxis de argumento
     * nombrado ("p_x => ?", "out => ?") que Postgres rechaza. Confirmado
     * end-to-end contra el stack local (revision tecnica 2026-09-05).
     */
    @Query(value = "SELECT fn_registrar_usuario(:p_nombres, :p_apellidos, :p_correo, :p_contrasena_hash, :p_fecha_nacimiento, :p_nombre_rol)", nativeQuery = true)
    Long registrarUsuario(
            @Param("p_nombres") String nombres,
            @Param("p_apellidos") String apellidos,
            @Param("p_correo") String correo,
            @Param("p_contrasena_hash") String contrasenaHash,
            @Param("p_fecha_nacimiento") LocalDate fechaNacimiento,
            @Param("p_nombre_rol") String nombreRol);

    /**
     * REQ-F-002 - fn_resolver_estado_login: estado de cuenta, 2FA y roles en
     * una sola llamada. Devuelve JSONB serializado como texto.
     *
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * Mismo motivo que permisosEfectivos (ver ahi el detalle completo):
     * @Procedure con retorno no-void rompe con Hibernate 7.4.1 contra
     * Postgres.
     */
    @Query(value = "SELECT fn_resolver_estado_login(:p_correo)::text", nativeQuery = true)
    String resolverEstadoLogin(@Param("p_correo") String correo);

    /**
     * REQ-F-005 - sp_restablecer_contrasena: valida token de recuperacion y
     * actualiza el hash de contrasena. PROCEDURE real (no FUNCTION): con
     * Hibernate 7, @Procedure + @Param nombrados contra una FUNCTION escalar
     * genera una llamada con sintaxis "p_x => ?" que Postgres no puede
     * parsear dentro del escape JDBC {call ...}. El caller ya descartaba el
     * valor de retorno (exito = no lanzo excepcion), asi que no hace falta
     * ningun parametro OUT -- a diferencia de las rutinas de este archivo que
     * SI necesitan devolver un valor (ver permisosEfectivos), este caso
     * calza con el unico patron de @Procedure verificado en el proyecto
     * (sp_registrar_decision_verificacion): solo parametros IN, metodo void.
     */
    @Procedure(procedureName = "sp_restablecer_contrasena")
    void restablecerContrasena(
            @Param("p_hash_token") String hashToken,
            @Param("p_nueva_contrasena_hash") String nuevaContrasenaHash);

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md ??5) -
     * fn_cambiar_estado_cuenta: cambia estado_cuenta y, si hay transicion
     * activa->inactiva, revoca las sesiones del usuario, todo bajo
     * SELECT ... FOR UPDATE sobre la misma transaccion. Cierra la actualizacion
     * perdida entre dos administradores operando el mismo usuario a la vez.
     * Devuelve las sesiones revocadas (vacio si no hubo transicion).
     *
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery]
     * Esta rutina devuelve un result set (TABLE) complejo proyectado en una interfaz Spring Data (DTO).
     * El mecanismo @Procedure (o @NamedStoredProcedureQuery) en PostgreSQL exige la devolucion de un RefCursor
     * como parametro OUT para mapear tablas, lo que colisiona con el soporte nativo de Proyecciones de Hibernate.
     * Por lo tanto, para funciones que devuelven multiples columnas como filas, nativeQuery=true es el mecanismo
     * recomendado y correcto que evita acoplar el esquema de BD a DTOs de mapeo hiper-estrictos.
     */
    @Query(value = "SELECT * FROM fn_cambiar_estado_cuenta(:p_id_usuario, :p_estado)", nativeQuery = true)
    List<SesionRevocadaProyeccion> cambiarEstadoCuenta(
            @Param("p_id_usuario") Long idUsuario,
            @Param("p_estado") boolean estado);

    /**
     * Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md ??8) -
     * fn_permisos_efectivos_usuario: resuelve usuario + authorities (roles
     * ROLE_* + permisos, deduplicados) en una sola llamada STABLE. Sustituye
     * el N+1 de CustomUserDetailsService.loadUserByUsername (findByCorreo +
     * findByUsuarioIdUsuario + un SELECT por rol via Rol.permisos EAGER),
     * ejecutado en CADA peticion autenticada. Devuelve JSONB serializado como
     * texto, NULL si el correo no existe.
     *
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * Esta es la rutina que en la practica bloqueaba TODO login (se ejecuta
     * en loadUserByUsername, antes que fn_resolver_estado_login). Se probo
     * convertirla a PROCEDURE con un parametro OUT (revision tecnica
     * 2026-09-05, ver commit de esa fecha): con Hibernate 7.4.1, en cuanto un
     * metodo @Procedure tiene un tipo de retorno no-void (mapeado a un OUT),
     * Hibernate registra TODOS los parametros -- incluido el propio OUT --
     * con sintaxis de argumento nombrado de Postgres, generando una llamada
     * invalida dentro del escape JDBC:
     *   {call sp_permisos_efectivos_usuario(p_correo => ?, out => ?)}
     * Postgres no puede parsear "=>" ahi (ERROR: syntax error at or near
     * "=>"), confirmado end-to-end contra el stack local (docker logs
     * pfc_backend, login real con admin@artisync.com). @Procedure en este
     * proyecto solo funciona de forma verificada cuando el metodo Java es
     * void y no hay ningun OUT (ver restablecerContrasena/cambiarContrasena
     * mas abajo, y sp_registrar_decision_verificacion). Para una FUNCTION
     * escalar con valor de retorno, @Query(nativeQuery=true) es la opcion
     * correcta y verificada.
     */
    @Query(value = "SELECT fn_permisos_efectivos_usuario(:p_correo)::text", nativeQuery = true)
    String permisosEfectivos(@Param("p_correo") String correo);

    /**
     * Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md ??6) -
     * fn_solicitar_recuperacion: invalida tokens de recuperacion previos e
     * inserta el nuevo atomicamente bajo SELECT FOR UPDATE (A5). Devuelve
     * JSONB {idUsuario, nombres} serializado como texto, NULL si la cuenta no
     * existe o esta inactiva (respuesta indistinguible preservada en Java).
     *
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * Mismo motivo que permisosEfectivos (ver ahi el detalle completo):
     * @Procedure con retorno no-void rompe con Hibernate 7.4.1 contra
     * Postgres.
     */
    @Query(value = "SELECT fn_solicitar_recuperacion(:p_correo, :p_hash_token)::text", nativeQuery = true)
    String solicitarRecuperacion(
            @Param("p_correo") String correo,
            @Param("p_hash_token") String hashToken);

    /**
     * Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md ??6) -
     * sp_cambiar_contrasena: UPDATE condicionado (compare-and-swap sobre el
     * hash) que aplica el cambio solo si nadie mas la cambio primero, cerrando
     * la actualizacion perdida (A7); lanza excepcion (ERRCODE 40001) si el
     * hash ya no coincidia. PROCEDURE real (no FUNCTION), mismo motivo que
     * sp_restablecer_contrasena de arriba: el caller ya descartaba el
     * booleano de retorno, asi que no hace falta ningun parametro OUT.
     */
    @Procedure(procedureName = "sp_cambiar_contrasena")
    void cambiarContrasena(
            @Param("p_id_usuario") Long idUsuario,
            @Param("p_hash_esperado") String hashEsperado,
            @Param("p_hash_nuevo") String hashNuevo);

    /**
     * Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md ??4) -
     * fn_crear_usuario_admin: crea un usuario administrativo con sus roles en
     * una unica transaccion, capturando unique_violation sobre el correo en
     * vez de una comprobacion existsByCorreo no atomica (A3). Devuelve el
     * id_usuario generado.
     *
     * [JUSTIFICACION ARQUITECTONICA - USO DE nativeQuery, no @Procedure]
     * Mismo motivo que permisosEfectivos (ver ahi el detalle completo):
     * @Procedure con retorno no-void rompe con Hibernate 7.4.1 contra
     * Postgres.
     */
    @Query(value = "SELECT fn_crear_usuario_admin(:p_nombres, :p_apellidos, :p_correo, :p_contrasena_hash, :p_fecha_nacimiento, :p_id_pais, :p_estado_cuenta, :p_nombres_rol)", nativeQuery = true)
    Long crearUsuarioAdmin(
            @Param("p_nombres") String nombres,
            @Param("p_apellidos") String apellidos,
            @Param("p_correo") String correo,
            @Param("p_contrasena_hash") String contrasenaHash,
            @Param("p_fecha_nacimiento") LocalDate fechaNacimiento,
            @Param("p_id_pais") Long idPais,
            @Param("p_estado_cuenta") Boolean estadoCuenta,
            @Param("p_nombres_rol") String[] nombresRol);
}
