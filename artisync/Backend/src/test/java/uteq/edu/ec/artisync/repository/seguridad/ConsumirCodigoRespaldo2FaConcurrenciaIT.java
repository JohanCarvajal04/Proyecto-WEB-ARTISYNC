package uteq.edu.ec.artisync.repository.seguridad;

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
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §11.2) para
 * fn_consumir_codigo_respaldo_2fa (Fase 1, anomalia A1).
 *
 * Reproduce contra Postgres real la condicion de carrera que tenia
 * TwoFactorServiceImpl.validarCodigoOBackup ANTES de esta rutina: N hilos
 * enviando el MISMO codigo de respaldo al mismo tiempo. Con el patron anterior
 * (leer todos los codigos no usados a memoria + comparar en Java + save()),
 * varios hilos podian leer usado = FALSE antes de que ninguno escribiera, y
 * todos devolvian TRUE. La aserción de esta prueba es sobre el INVARIANTE de
 * negocio -- "un codigo de un solo uso se consume una sola vez" -- no sobre el
 * orden de ejecucion, que es no determinista.
 *
 * Requiere Postgres real: el UPDATE atomico y el bloqueo de fila que lo hacen
 * seguro bajo concurrencia son comportamiento del motor, no reproducible
 * contra H2 (perfil de test por defecto). Se desactiva el @Transactional que
 * @DataJpaTest aplica por defecto (Propagation.NOT_SUPPORTED): cada hilo
 * concurrente necesita su PROPIA conexion/transaccion real tomada del pool,
 * no una unica transaccion de prueba compartida que @DataJpaTest revertiria
 * al final del metodo.
 *
 * Ejecutar con:
 *   ./mvnw test -Dtest=ConsumirCodigoRespaldo2FaConcurrenciaIT -Dspring.profiles.active=postgres-it
 * (requiere docker compose -f artisync/docker-compose.yml up -d postgres)
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConsumirCodigoRespaldo2FaConcurrenciaIT {

    private static final long ID_USUARIO = 9101L;
    private static final String CODIGO_HASH = "hash-concurrencia-test";
    private static final int HILOS = 10;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM codigos_respaldo_2fa WHERE id_usuario = ?", ID_USUARIO);
        jdbcTemplate.update("DELETE FROM usuarios WHERE id_usuario = ?", ID_USUARIO);
    }

    @Test
    void soloUnHiloConsumeElCodigoDeRespaldo() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Test', 'Concurrencia', 'concurrencia-2fa@test.dev', 'x', true)",
                ID_USUARIO);
        jdbcTemplate.update(
                "INSERT INTO codigos_respaldo_2fa (id_usuario, codigo_hash, usado) VALUES (?, ?, false)",
                ID_USUARIO, CODIGO_HASH);

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listos = new CountDownLatch(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();

        for (int i = 0; i < HILOS; i++) {
            pool.submit(() -> {
                try (Connection conexion = dataSource.getConnection();
                     PreparedStatement stmt = conexion.prepareStatement(
                             "SELECT fn_consumir_codigo_respaldo_2fa(?, ?)")) {
                    stmt.setLong(1, ID_USUARIO);
                    stmt.setString(2, CODIGO_HASH);

                    listos.countDown();
                    salida.await(10, TimeUnit.SECONDS); // arranque simultaneo de los N hilos

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getBoolean(1)) {
                            exitos.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        listos.await(10, TimeUnit.SECONDS);
        salida.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Invariante de negocio: exactamente un llamante consumio el codigo,
        // sin importar cual (el orden entre hilos es no determinista).
        assertThat(exitos.get()).isEqualTo(1);

        Boolean usado = jdbcTemplate.queryForObject(
                "SELECT usado FROM codigos_respaldo_2fa WHERE id_usuario = ? AND codigo_hash = ?",
                Boolean.class, ID_USUARIO, CODIGO_HASH);
        assertThat(usado).isTrue();
    }
}
