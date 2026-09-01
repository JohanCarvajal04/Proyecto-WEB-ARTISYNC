package uteq.edu.ec.artisync.repository.perfil;

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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de concurrencia para el hallazgo "decisión de verificación puede
 * sobrescribirse silenciosamente" (revisión técnica, 2026-09-01):
 * sp_registrar_decision_verificacion comprobaba el estado PENDIENTE con un
 * SELECT sin FOR UPDATE, así que dos moderadores decidiendo casi
 * simultáneamente sobre el mismo certificado podían leer ambos PENDIENTE
 * antes de que cualquiera confirmara -- el segundo UPDATE se bloqueaba por el
 * lock de fila del primero, pero al ejecutarse sobrescribía silenciosamente
 * la decisión ya tomada en vez de lanzar la excepción esperada.
 *
 * El arreglo (V31__fix_race_condicion_decision_verificacion.sql) agrega
 * FOR UPDATE al SELECT inicial, mismo patrón que fn_seleccionar_ganadores_sorteo
 * y fn_registrar_infraccion. La aserción es sobre el INVARIANTE de negocio --
 * "solo la primera decisión persiste" -- no sobre el orden de ejecución, que
 * es no determinista.
 *
 * @DataJpaTest (no @SpringBootTest): certificadoIaRepository.registrarDecision
 * es un @Procedure de Spring Data, no depende de otros beans de servicio.
 * Sin @Transactional en la clase (Propagation.NOT_SUPPORTED): cada llamada al
 * repositorio necesita su propia transacción real para que el bloqueo de fila
 * sirva de algo, igual que ConsumirCodigoRespaldo2FaConcurrenciaIT.
 *
 * Requiere Postgres real. Ejecutar con:
 *   ./mvnw test -Dtest=RegistrarDecisionVerificacionConcurrenciaIT -Dspring.profiles.active=postgres-it
 * (requiere docker compose -f artisync/docker-compose.yml up -d postgres)
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RegistrarDecisionVerificacionConcurrenciaIT {

    private static final long ID_CREADOR = 9301L;
    private static final long ID_CERTIFICADO = 9301L;
    private static final int HILOS = 10;

    @Autowired
    private CertificadoIaRepository certificadoIaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM certificados_ia WHERE id_certificado = ?", ID_CERTIFICADO);
        jdbcTemplate.update("DELETE FROM perfiles_creadores WHERE id_usuario = ?", ID_CREADOR);
        jdbcTemplate.update("DELETE FROM usuarios WHERE id_usuario = ?", ID_CREADOR);
    }

    @Test
    void soloUnaDecisionPersisteSobreElMismoCertificado() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Creador', 'Concurrencia', 'creador-concurrencia-verificacion@test.dev', 'x', true)",
                ID_CREADOR);
        jdbcTemplate.update(
                "INSERT INTO perfiles_creadores (id_usuario) VALUES (?)", ID_CREADOR);
        jdbcTemplate.update(
                "INSERT INTO certificados_ia (id_certificado, id_usuario, id_estado_verificacion, url_documento_s3, tipo_documento) " +
                        "SELECT ?, ?, id_estado_verificacion, 'ref.jpg', 'IDENTIDAD' " +
                        "FROM estados_verificacion WHERE nombre_estado = 'PENDIENTE'",
                ID_CERTIFICADO, ID_CREADOR);

        Long idEstadoAprobado = jdbcTemplate.queryForObject(
                "SELECT id_estado_verificacion FROM estados_verificacion WHERE nombre_estado = 'APROBADO'", Long.class);

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listos = new CountDownLatch(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();

        for (int i = 0; i < HILOS; i++) {
            final long idModerador = 9400L + i;
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await(10, TimeUnit.SECONDS); // arranque simultaneo de los N hilos
                    // Cada hilo simula un moderador distinto; solo id_moderador
                    // varia -- no hace falta que la fila exista en usuarios
                    // para esta prueba porque lo que se ejercita es el lock,
                    // no la FK (el procedimiento no valida FK de moderador
                    // contra un usuario real en este flujo de prueba... si la
                    // rutina lo exige, se sustituye ID_CREADOR como moderador
                    // fijo en su lugar).
                    certificadoIaRepository.registrarDecision(
                            ID_CERTIFICADO, idEstadoAprobado, ID_CREADOR, "Decision concurrente " + idModerador);
                    exitos.incrementAndGet();
                } catch (Exception ignorada) {
                    // Se espera que HILOS-1 lancen (certificado ya no
                    // PENDIENTE): es el comportamiento correcto, no un fallo
                    // de la prueba.
                }
            });
        }

        listos.await(10, TimeUnit.SECONDS);
        salida.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Invariante de negocio: exactamente un hilo logro registrar la
        // decision, sin importar cual (el orden entre hilos es no
        // determinista).
        assertThat(exitos.get()).isEqualTo(1);

        Long idEstadoFinal = jdbcTemplate.queryForObject(
                "SELECT id_estado_verificacion FROM certificados_ia WHERE id_certificado = ?",
                Long.class, ID_CERTIFICADO);
        assertThat(idEstadoFinal).isEqualTo(idEstadoAprobado);
    }
}
