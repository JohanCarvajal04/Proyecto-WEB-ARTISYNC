package uteq.edu.ec.artisync.repository.legal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de concurrencia para uq_solicitud_retiro_pendiente_por_creador
 * (V34__modulo_retiros.sql): SolicitudRetiroServicioImpl.solicitar() valida
 * en Java que el creador no tenga ya una solicitud Pendiente/Aprobada antes
 * de insertar, pero ese SELECT no bloquea fila (a diferencia de
 * findByIdParaActualizar, que sí usa PESSIMISTIC_WRITE para aprobar/rechazar).
 * Dos solicitudes casi simultáneas del mismo creador podrían leer "no hay
 * ninguna en curso" antes de que cualquiera insertara. El índice único parcial
 * es la última línea de defensa: esta prueba lo ejercita directamente contra
 * Postgres real, sin pasar por el servicio, para confirmar que efectivamente
 * solo una fila puede quedar en curso por creador sin importar la carrera.
 *
 * Requiere Postgres real (el índice único parcial es comportamiento del
 * motor, no reproducible de forma fiable contra H2). Ejecutar con:
 *   ./mvnw test -Dtest=SolicitudRetiroConcurrenciaIT -Dspring.profiles.active=postgres-it
 * (requiere docker compose -f artisync/docker-compose.yml up -d postgres)
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SolicitudRetiroConcurrenciaIT {

    private static final long ID_CREADOR = 9301L;
    private static final int HILOS = 10;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM solicitudes_retiro WHERE id_usuario_creador = ?", ID_CREADOR);
        jdbcTemplate.update("DELETE FROM usuarios WHERE id_usuario = ?", ID_CREADOR);
    }

    @Test
    void soloUnaSolicitudEnCursoPorCreador() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Creador', 'Concurrencia', 'creador-concurrencia-retiro@test.dev', 'x', true)",
                ID_CREADOR);

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listos = new CountDownLatch(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();

        for (int i = 0; i < HILOS; i++) {
            pool.submit(() -> {
                try (Connection conexion = dataSource.getConnection();
                     PreparedStatement stmt = conexion.prepareStatement(
                             "INSERT INTO solicitudes_retiro " +
                                     "(id_usuario_creador, monto_solicitado, correo_paypal_destino, estado) " +
                                     "VALUES (?, 20.00, 'creador-concurrencia-retiro@test.dev', 'Pendiente')")) {
                    stmt.setLong(1, ID_CREADOR);

                    listos.countDown();
                    salida.await(10, TimeUnit.SECONDS); // arranque simultaneo de los N hilos

                    stmt.executeUpdate();
                    exitos.incrementAndGet();
                } catch (Exception ignorada) {
                    // Se espera que HILOS-1 violen uq_solicitud_retiro_pendiente_por_creador:
                    // es el comportamiento correcto, no un fallo de la prueba.
                }
            });
        }

        listos.await(10, TimeUnit.SECONDS);
        salida.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Invariante de negocio: exactamente una solicitud quedó registrada,
        // sin importar cuál de los hilos ganó la carrera (no determinista).
        assertThat(exitos.get()).isEqualTo(1);

        Integer enCurso = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM solicitudes_retiro " +
                        "WHERE id_usuario_creador = ? AND estado IN ('Pendiente', 'Aprobado')",
                Integer.class, ID_CREADOR);
        assertThat(enCurso).isEqualTo(1);
    }
}
