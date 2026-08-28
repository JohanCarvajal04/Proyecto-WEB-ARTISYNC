package uteq.edu.ec.artisync.repository.comunicacion;

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
 * H-08 (auditoría de estado 2026-08-26): sp_purgar_notificaciones es un
 * PROCEDURE con COMMIT interno por lote, así que no puede invocarse dentro
 * de la transacción de prueba que @DataJpaTest envuelve por defecto
 * (fallaría con 2D000 invalid_transaction_termination, el mismo motivo por
 * el que NotificacionesPurgaScheduler usa Propagation.NOT_SUPPORTED en
 * producción) — de ahí {@code @Transactional(propagation = NOT_SUPPORTED)}
 * a nivel de clase.
 *
 * Requiere Postgres real (PL/pgSQL con COMMIT/ROLLBACK no es soportado por
 * H2, el perfil de test por defecto).
 *
 * Ejecutar con:
 *   ./mvnw test -Dtest=SpPurgarNotificacionesIT -Dspring.profiles.active=postgres-it
 * (requiere docker compose -f artisync/docker-compose.yml up -d postgres)
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpPurgarNotificacionesIT {

    private static final long ID_USUARIO = 9301L;
    private static final long ID_TIPO_NOTIFICACION = 9301L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM notificaciones_sistema WHERE id_usuario = ?", ID_USUARIO);
        jdbcTemplate.update("DELETE FROM tipos_notificacion WHERE id_tipo_notificacion = ?", ID_TIPO_NOTIFICACION);
        jdbcTemplate.update("DELETE FROM usuarios WHERE id_usuario = ?", ID_USUARIO);
    }

    private void sembrarUsuarioYTipo() {
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Test', 'Purga', 'purga-notificaciones@test.dev', 'x', true)",
                ID_USUARIO);
        jdbcTemplate.update(
                "INSERT INTO tipos_notificacion (id_tipo_notificacion, nombre_evento) VALUES (?, 'TEST_PURGA_NOTIFICACIONES')",
                ID_TIPO_NOTIFICACION);
    }

    @Test
    @DisplayName("purga notificaciones leidas con más de la retención configurada, conserva una leída reciente")
    void purgaNotificacionesLeidasViejas_conservaLeidaReciente() {
        sembrarUsuarioYTipo();
        jdbcTemplate.update(
                "INSERT INTO notificaciones_sistema (id_usuario, id_tipo_notificacion, esta_leida, fecha_emision) " +
                        "VALUES (?, ?, true, CURRENT_TIMESTAMP - INTERVAL '100 days')",
                ID_USUARIO, ID_TIPO_NOTIFICACION);
        jdbcTemplate.update(
                "INSERT INTO notificaciones_sistema (id_usuario, id_tipo_notificacion, esta_leida, fecha_emision) " +
                        "VALUES (?, ?, true, CURRENT_TIMESTAMP - INTERVAL '1 day')",
                ID_USUARIO, ID_TIPO_NOTIFICACION);

        jdbcTemplate.update("CALL sp_purgar_notificaciones(?, ?)", 1000, 90);

        Integer restantes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notificaciones_sistema WHERE id_usuario = ?", Integer.class, ID_USUARIO);

        assertThat(restantes).isEqualTo(1);
    }

    @Test
    @DisplayName("NUNCA purga una notificación no leída, sin importar su antigüedad")
    void nuncaPurgaNoLeidas_sinImportarAntiguedad() {
        // Invariante critico de diseño (ver cabecera de sp_purgar_notificaciones.sql):
        // el usuario todavia no vio esta notificacion, sin importar cuanto tiempo lleve sin leerla.
        sembrarUsuarioYTipo();
        jdbcTemplate.update(
                "INSERT INTO notificaciones_sistema (id_usuario, id_tipo_notificacion, esta_leida, fecha_emision) " +
                        "VALUES (?, ?, false, CURRENT_TIMESTAMP - INTERVAL '365 days')",
                ID_USUARIO, ID_TIPO_NOTIFICACION);

        jdbcTemplate.update("CALL sp_purgar_notificaciones(?, ?)", 1000, 90);

        Integer restantes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notificaciones_sistema WHERE id_usuario = ?", Integer.class, ID_USUARIO);

        assertThat(restantes).isEqualTo(1);
    }
}
