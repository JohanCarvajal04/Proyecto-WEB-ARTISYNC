package uteq.edu.ec.artisync.repository.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uteq.edu.ec.artisync.entity.security.User;
import uteq.edu.ec.artisync.repository.support.PgArrays;

import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

/**
 * Invoca las funciones SQL con retorno no-void de {@link User} con
 * {@link NamedParameterJdbcTemplate} en vez de {@code @Query(nativeQuery = true)} (P6 de la guía
 * del examen suspenso). Ver {@code docs/basedatos/CATALOGO-SP.md} §14: {@code @Procedure} con
 * retorno no-void rompe contra Postgres bajo Hibernate 7.4.x (un intento anterior de convertir
 * estas mismas rutinas rompió el login en producción el 4-sep-2026, revertido el mismo día).
 * {@code NamedParameterJdbcTemplate} nunca pasa por el traductor de Hibernate que causa ese bug:
 * construye {@code SELECT fn_x(?, ?, ...)} con parámetros posicionales JDBC estándar.
 */
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private record RevokedSessionRow(String jti, Integer segundosRestantes) implements RevokedSessionProjection {

        /** @return el JTI (identificador único) de la sesión revocada */
        @Override
        public String getJti() {
            return jti;
        }

        /** @return los segundos restantes de vigencia del token en el momento de la revocación */
        @Override
        public Integer getSegundosRestantes() {
            return segundosRestantes;
        }
    }

    /**
     * @param nombres nombres del usuario
     * @param apellidos apellidos del usuario
     * @param correo correo del usuario, debe ser único
     * @param contrasenaHash hash BCrypt de la contraseña
     * @param fechaNacimiento fecha de nacimiento del usuario
     * @param nombreRol nombre del rol inicial a asignar
     * @return el id_usuario generado
     */
    @Override
    public Long registrarUsuario(String nombres, String apellidos, String correo, String contrasenaHash,
                                  LocalDate fechaNacimiento, String nombreRol) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_nombres", nombres)
                .addValue("p_apellidos", apellidos)
                .addValue("p_correo", correo)
                .addValue("p_contrasena_hash", contrasenaHash)
                .addValue("p_fecha_nacimiento", fechaNacimiento)
                .addValue("p_nombre_rol", nombreRol);
        return jdbcTemplate.queryForObject(
                "SELECT fn_registrar_usuario(:p_nombres, :p_apellidos, :p_correo, :p_contrasena_hash, "
                        + ":p_fecha_nacimiento, :p_nombre_rol)",
                params, Long.class);
    }

    /**
     * @param correo correo del usuario que intenta autenticarse
     * @return JSONB serializado como texto con estado de cuenta, 2FA y roles
     */
    @Override
    public String resolverEstadoLogin(String correo) {
        MapSqlParameterSource params = new MapSqlParameterSource("p_correo", correo);
        return jdbcTemplate.queryForObject(
                "SELECT fn_resolver_estado_login(:p_correo)::text", params, String.class);
    }

    /**
     * @param idUsuario identificador del usuario
     * @param estado nuevo valor de estado_cuenta
     * @return las sesiones revocadas si hubo transición activa→inactiva, vacío en otro caso
     */
    @Override
    public List<RevokedSessionProjection> cambiarEstadoCuenta(Long idUsuario, boolean estado) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_id_usuario", idUsuario)
                .addValue("p_estado", estado);
        return jdbcTemplate.query(
                "SELECT * FROM fn_cambiar_estado_cuenta(:p_id_usuario, :p_estado)",
                params,
                (rs, rowNum) -> new RevokedSessionRow(
                        rs.getString("jti"),
                        rs.getObject("segundos_restantes", Integer.class)));
    }

    /**
     * @param correo correo del usuario autenticado
     * @return JSONB serializado como texto con usuario + authorities, o {@code NULL} si no existe
     */
    @Override
    public String permisosEfectivos(String correo) {
        MapSqlParameterSource params = new MapSqlParameterSource("p_correo", correo);
        return jdbcTemplate.queryForObject(
                "SELECT fn_permisos_efectivos_usuario(:p_correo)::text", params, String.class);
    }

    /**
     * @param correo correo del usuario que solicita recuperar su contraseña
     * @param hashToken hash del nuevo token de recuperación
     * @return JSONB serializado como texto con {@code idUsuario, nombres}, o {@code NULL} si no aplica
     */
    @Override
    public String solicitarRecuperacion(String correo, String hashToken) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_correo", correo)
                .addValue("p_hash_token", hashToken);
        return jdbcTemplate.queryForObject(
                "SELECT fn_solicitar_recuperacion(:p_correo, :p_hash_token)::text", params, String.class);
    }

    /**
     * @param nombres nombres del usuario
     * @param apellidos apellidos del usuario
     * @param correo correo del usuario, debe ser único
     * @param contrasenaHash hash BCrypt de la contraseña
     * @param fechaNacimiento fecha de nacimiento del usuario
     * @param idPais identificador del país del usuario, o {@code null}
     * @param estadoCuenta estado de la cuenta a asignar; {@code null} equivale a activa
     * @param nombresRol nombres de los roles a asignar
     * @return el id_usuario generado
     */
    @Override
    public Long crearUsuarioAdmin(String nombres, String apellidos, String correo, String contrasenaHash,
                                   LocalDate fechaNacimiento, Long idPais, Boolean estadoCuenta,
                                   String[] nombresRol) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_nombres", nombres)
                .addValue("p_apellidos", apellidos)
                .addValue("p_correo", correo)
                .addValue("p_contrasena_hash", contrasenaHash)
                .addValue("p_fecha_nacimiento", fechaNacimiento)
                .addValue("p_id_pais", idPais, Types.BIGINT)
                .addValue("p_estado_cuenta", estadoCuenta)
                .addValue("p_nombres_rol", PgArrays.textArray(jdbcTemplate, nombresRol));
        return jdbcTemplate.queryForObject(
                "SELECT fn_crear_usuario_admin(:p_nombres, :p_apellidos, :p_correo, :p_contrasena_hash, "
                        + ":p_fecha_nacimiento, :p_id_pais, :p_estado_cuenta, :p_nombres_rol)",
                params, Long.class);
    }
}
