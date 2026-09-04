package uteq.edu.ec.artisync.repository.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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

    

    /** REQ-F-001 - fn_registrar_usuario: inserta usuario + usuario_roles + perfil de creador opcional. Devuelve el id_usuario generado. */
    @Procedure(procedureName = "fn_registrar_usuario")
    Long registrarUsuario(
            @Param("p_nombres") String nombres,
            @Param("p_apellidos") String apellidos,
            @Param("p_correo") String correo,
            @Param("p_contrasena_hash") String contrasenaHash,
            @Param("p_fecha_nacimiento") LocalDate fechaNacimiento,
            @Param("p_nombre_rol") String nombreRol);

    /** REQ-F-002 - fn_resolver_estado_login: estado de cuenta, 2FA y roles en una sola llamada. Devuelve JSONB serializado como texto. */
    @Procedure(procedureName = "fn_resolver_estado_login")
    String resolverEstadoLogin(@Param("p_correo") String correo);

    /** REQ-F-005 - fn_restablecer_contrasena: valida token de recuperacion y actualiza el hash de contrasena. Devuelve el id_usuario afectado. */
    @Procedure(procedureName = "fn_restablecer_contrasena")
    Long restablecerContrasena(
            @Param("p_hash_token") String hashToken,
            @Param("p_nueva_contrasena_hash") String nuevaContrasenaHash);

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5) -
     * fn_cambiar_estado_cuenta: cambia estado_cuenta y, si hay transicion
     * activa->inactiva, revoca las sesiones del usuario, todo bajo
     * SELECT ... FOR UPDATE sobre la misma transaccion. Cierra la actualizacion
     * perdida entre dos administradores operando el mismo usuario a la vez.
     * Devuelve las sesiones revocadas (vacio si no hubo transicion).
     */
    @Query(value = "SELECT * FROM fn_cambiar_estado_cuenta(:p_id_usuario, :p_estado)", nativeQuery = true)
    List<SesionRevocadaProyeccion> cambiarEstadoCuenta(
            @Param("p_id_usuario") Long idUsuario,
            @Param("p_estado") boolean estado);

    /**
     * Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §8) -
     * fn_permisos_efectivos_usuario: resuelve usuario + authorities (roles
     * ROLE_* + permisos, deduplicados) en una sola llamada STABLE. Sustituye
     * el N+1 de CustomUserDetailsService.loadUserByUsername (findByCorreo +
     * findByUsuarioIdUsuario + un SELECT por rol via Rol.permisos EAGER),
     * ejecutado en CADA peticion autenticada. Devuelve JSONB serializado como
     * texto, NULL si el correo no existe.
     */
    @Procedure(procedureName = "fn_permisos_efectivos_usuario")
    String permisosEfectivos(@Param("p_correo") String correo);

    /**
     * Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §6) -
     * fn_solicitar_recuperacion: invalida tokens de recuperacion previos e
     * inserta el nuevo atomicamente bajo SELECT FOR UPDATE (A5). Devuelve
     * JSONB {idUsuario, nombres} serializado como texto, NULL si la cuenta no
     * existe o esta inactiva (respuesta indistinguible preservada en Java).
     */
    @Procedure(procedureName = "fn_solicitar_recuperacion")
    String solicitarRecuperacion(
            @Param("p_correo") String correo,
            @Param("p_hash_token") String hashToken);

    /**
     * Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §6) -
     * fn_cambiar_contrasena: UPDATE condicionado (compare-and-swap sobre el
     * hash) que aplica el cambio solo si nadie mas la cambio primero, cerrando
     * la actualizacion perdida (A7). Devuelve TRUE si se aplico; lanza
     * excepcion (ERRCODE 40001) si el hash ya no coincidia.
     */
    @Procedure(procedureName = "fn_cambiar_contrasena")
    Boolean cambiarContrasena(
            @Param("p_id_usuario") Long idUsuario,
            @Param("p_hash_esperado") String hashEsperado,
            @Param("p_hash_nuevo") String hashNuevo);

    /**
     * Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §4) -
     * fn_crear_usuario_admin: crea un usuario administrativo con sus roles en
     * una unica transaccion, capturando unique_violation sobre el correo en
     * vez de una comprobacion existsByCorreo no atomica (A3). Devuelve el
     * id_usuario generado.
     */
    @Procedure(procedureName = "fn_crear_usuario_admin")
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

