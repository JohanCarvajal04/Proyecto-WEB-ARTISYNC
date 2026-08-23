package uteq.edu.ec.artisync.repository.seguridad;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fase 4 mantenimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §7):
 * sp_purgar_datos_seguridad es un PROCEDURE con COMMIT interno por lote, así
 * que no puede invocarse dentro de la transacción de prueba que @DataJpaTest
 * envuelve por defecto (fallaría con 2D000 invalid_transaction_termination,
 * el mismo motivo por el que SeguridadPurgaScheduler usa
 * Propagation.NOT_SUPPORTED en producción) — de ahí
 * {@code @Transactional(propagation = NOT_SUPPORTED)} a nivel de clase.
 *
 * Requiere Postgres real (PL/pgSQL con COMMIT/ROLLBACK no es soportado por
 * H2, el perfil de test por defecto).
 *
 * Ejecutar con:
 *   ./mvnw test -Dtest=SpPurgarDatosSeguridadIT -Dspring.profiles.active=postgres-it
 * (requiere docker compose -f artisync/docker-compose.yml up -d postgres)
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpPurgarDatosSeguridadIT {

    private static final long ID_USUARIO = 9201L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM sesiones_usuario WHERE id_usuario = ?", ID_USUARIO);
        jdbcTemplate.update("DELETE FROM tokens_recuperacion WHERE id_usuario = ?", ID_USUARIO);
        jdbcTemplate.update("DELETE FROM codigos_respaldo_2fa WHERE id_usuario = ?", ID_USUARIO);
        jdbcTemplate.update("DELETE FROM usuarios WHERE id_usuario = ?", ID_USUARIO);
    }

    @Test
    @DisplayName("purga sesiones expiradas y conserva las sesiones vigentes")
    void purgaSesionesExpiradas_conservaVigentes() {
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Test', 'Purga', 'purga-sesiones@test.dev', 'x', true)",
                ID_USUARIO);
        jdbcTemplate.update(
                "INSERT INTO sesiones_usuario (id_usuario, jti, fecha_expiracion) " +
                        "VALUES (?, 'jti-expirado', CURRENT_TIMESTAMP - INTERVAL '1 hour')",
                ID_USUARIO);
        jdbcTemplate.update(
                "INSERT INTO sesiones_usuario (id_usuario, jti, fecha_expiracion) " +
                        "VALUES (?, 'jti-vigente', CURRENT_TIMESTAMP + INTERVAL '1 hour')",
                ID_USUARIO);

        jdbcTemplate.update("CALL sp_purgar_datos_seguridad(?)", 1000);

        Integer restantes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sesiones_usuario WHERE id_usuario = ?", Integer.class, ID_USUARIO);
        String jtiRestante = jdbcTemplate.queryForObject(
                "SELECT jti FROM sesiones_usuario WHERE id_usuario = ?", String.class, ID_USUARIO);

        assertThat(restantes).isEqualTo(1);
        assertThat(jtiRestante).isEqualTo("jti-vigente");
    }

    @Test
    @DisplayName("purga tokens de recuperación usados o con más de 24h, conserva uno reciente sin usar")
    void purgaTokensRecuperacionMuertos_conservaVigente() {
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Test', 'Purga', 'purga-tokens@test.dev', 'x', true)",
                ID_USUARIO);
        jdbcTemplate.update(
                "INSERT INTO tokens_recuperacion (id_usuario, hash_token, usado, fecha_generacion) " +
                        "VALUES (?, 'hash-usado', true, CURRENT_TIMESTAMP)",
                ID_USUARIO);
        jdbcTemplate.update(
                "INSERT INTO tokens_recuperacion (id_usuario, hash_token, usado, fecha_generacion) " +
                        "VALUES (?, 'hash-viejo', false, CURRENT_TIMESTAMP - INTERVAL '25 hours')",
                ID_USUARIO);
        jdbcTemplate.update(
                "INSERT INTO tokens_recuperacion (id_usuario, hash_token, usado, fecha_generacion) " +
                        "VALUES (?, 'hash-reciente', false, CURRENT_TIMESTAMP)",
                ID_USUARIO);

        jdbcTemplate.update("CALL sp_purgar_datos_seguridad(?)", 1000);

        String hashRestante = jdbcTemplate.queryForObject(
                "SELECT hash_token FROM tokens_recuperacion WHERE id_usuario = ?", String.class, ID_USUARIO);

        assertThat(hashRestante).isEqualTo("hash-reciente");
    }

    @Test
    @DisplayName("purga codigos de respaldo 2FA consumidos, pero NUNCA los no usados (setup2Fa en curso)")
    void purgaCodigosRespaldoConsumidos_nuncaLosNoUsados() {
        // Invariante critico de disenio (ver cabecera de sp_purgar_datos_seguridad.sql):
        // un codigo no usado no se purga por su antiguedad porque no hay forma
        // de distinguir "2FA desactivado hace tiempo" de "fn_configurar_2fa
        // recien genero estos codigos y el usuario aun no llamo a confirm2Fa".
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Test', 'Purga', 'purga-2fa@test.dev', 'x', true)",
                ID_USUARIO);
        jdbcTemplate.update(
                "INSERT INTO codigos_respaldo_2fa (id_usuario, codigo_hash, usado) VALUES (?, 'hash-consumido', true)",
                ID_USUARIO);
        jdbcTemplate.update(
                "INSERT INTO codigos_respaldo_2fa (id_usuario, codigo_hash, usado) VALUES (?, 'hash-sin-usar', false)",
                ID_USUARIO);

        jdbcTemplate.update("CALL sp_purgar_datos_seguridad(?)", 1000);

        String hashRestante = jdbcTemplate.queryForObject(
                "SELECT codigo_hash FROM codigos_respaldo_2fa WHERE id_usuario = ?", String.class, ID_USUARIO);

        assertThat(hashRestante).isEqualTo("hash-sin-usar");
    }
}
