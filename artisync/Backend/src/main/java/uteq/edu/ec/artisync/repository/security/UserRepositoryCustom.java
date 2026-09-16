package uteq.edu.ec.artisync.repository.security;

import uteq.edu.ec.artisync.entity.security.User;

import java.time.LocalDate;
import java.util.List;

/**
 * Invocaciones de las funciones SQL de {@link User} que devuelven un valor (escalar, JSONB o
 * TABLE) vía JDBC directo, fuera del mecanismo de {@code @Query} de Spring Data JPA (ver
 * {@link UserRepositoryImpl}). {@code restablecerContrasena} y {@code cambiarContrasena} NO están
 * aquí: son {@code void} sin parámetro {@code OUT}, el único patrón verificado sin fallos contra
 * Hibernate 7.4.x — se quedan como {@code @Procedure} en {@link UserRepository}.
 */
public interface UserRepositoryCustom {

    /**
     * REQ-F-001 - fn_registrar_usuario: inserta usuario + usuario_roles + perfil de creador opcional. Devuelve el id_usuario generado.
     * @param nombres los nombres
     * @param apellidos los apellidos
     * @param correo el correo
     * @param contrasenaHash el contrasena hash
     * @param fechaNacimiento la fecha de nacimiento
     * @param nombreRol el nombre de rol
     * @return el valor numerico calculado
     */
    Long registrarUsuario(String nombres, String apellidos, String correo, String contrasenaHash,
                           LocalDate fechaNacimiento, String nombreRol);

    /**
     * REQ-F-002 - fn_resolver_estado_login: estado de cuenta, 2FA y roles en una sola llamada. Devuelve JSONB serializado como texto.
     * @param correo el correo
     * @return el resultado en forma de texto
     */
    String resolverEstadoLogin(String correo);

    /**
     * fn_cambiar_estado_cuenta: cambia estado_cuenta y revoca sesiones si hubo transición activa->inactiva.
     * @param idUsuario el identificador de usuario
     * @param estado el estado
     * @return la lista de RevokedSessionProjection encontrados
     */
    List<RevokedSessionProjection> cambiarEstadoCuenta(Long idUsuario, boolean estado);

    /**
     * fn_permisos_efectivos_usuario: resuelve usuario + authorities en una sola llamada STABLE. Se usa en CADA petición autenticada.
     * @param correo el correo
     * @return el resultado en forma de texto
     */
    String permisosEfectivos(String correo);

    /**
     * fn_solicitar_recuperacion: invalida tokens de recuperación previos e inserta el nuevo atómicamente.
     * @param correo el correo
     * @param hashToken el hash token
     * @return el resultado en forma de texto
     */
    String solicitarRecuperacion(String correo, String hashToken);

    /**
     * fn_crear_usuario_admin: crea un usuario administrativo con sus roles en una única transacción. Devuelve el id_usuario generado.
     * @param nombres los nombres
     * @param apellidos los apellidos
     * @param correo el correo
     * @param contrasenaHash el contrasena hash
     * @param fechaNacimiento la fecha de nacimiento
     * @param idPais el identificador de pais
     * @param estadoCuenta el estado de cuenta
     * @param nombresRol el nombres rol
     * @return el valor numerico calculado
     */
    Long crearUsuarioAdmin(String nombres, String apellidos, String correo, String contrasenaHash,
                           LocalDate fechaNacimiento, Long idPais, Boolean estadoCuenta, String[] nombresRol);
}
