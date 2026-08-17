package uteq.edu.ec.artisync.repository.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByIdUsuarioAndEstadoCuentaTrue(Long idUsuario);

    boolean existsByPaisIdPais(Long idPais);

    // Las cuatro rutinas siguientes se invocan con @Query nativa (SELECT fn_x(...)),
    // no con @Procedure: son FUNCTION de PostgreSQL, y el unico ejemplo probado en
    // este repositorio de una FUNCTION invocada desde Spring Data usa este patron
    // (ver CertificadoIaRepository.listarCola / fn_listar_cola_verificacion). @Procedure
    // solo se ha verificado en este proyecto contra un CREATE PROCEDURE real
    // (sp_registrar_decision_verificacion).

    /** REQ-F-001 - fn_registrar_usuario: inserta usuario + usuario_roles + perfil de creador opcional. Devuelve el id_usuario generado. */
    @Query(value = "SELECT fn_registrar_usuario(:p_nombres, :p_apellidos, :p_correo, :p_contrasena_hash, :p_fecha_nacimiento, :p_nombre_rol)", nativeQuery = true)
    Long registrarUsuario(
            @Param("p_nombres") String nombres,
            @Param("p_apellidos") String apellidos,
            @Param("p_correo") String correo,
            @Param("p_contrasena_hash") String contrasenaHash,
            @Param("p_fecha_nacimiento") LocalDate fechaNacimiento,
            @Param("p_nombre_rol") String nombreRol);

    /** REQ-F-002 - fn_resolver_estado_login: estado de cuenta, 2FA y roles en una sola llamada. Devuelve JSONB serializado como texto. */
    @Query(value = "SELECT fn_resolver_estado_login(:p_correo)::text", nativeQuery = true)
    String resolverEstadoLogin(@Param("p_correo") String correo);

    /** REQ-F-005 - fn_restablecer_contrasena: valida token de recuperacion y actualiza el hash de contrasena. Devuelve el id_usuario afectado. */
    @Query(value = "SELECT fn_restablecer_contrasena(:p_hash_token, :p_nueva_contrasena_hash)", nativeQuery = true)
    Long restablecerContrasena(
            @Param("p_hash_token") String hashToken,
            @Param("p_nueva_contrasena_hash") String nuevaContrasenaHash);
}
