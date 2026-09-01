package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import uteq.edu.ec.artisync.service.legal.IEntregableServicio;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de concurrencia para el hallazgo "el escrow puede liberarse dos
 * veces" (revisión técnica, 2026-09-01): EntregableServicioImpl.aprobarEntrega
 * comprobaba entregable.getEstaLiberado() sin bloqueo de fila, así que dos
 * peticiones simultaneas (doble clic, reintento de red, dos pestañas) podían
 * leer estaLiberado=false antes de que ninguna confirmara, duplicando las
 * transacciones de "Egreso"/"Comision" en el libro contable.
 *
 * El arreglo usa EntregableFinalRepository.findByPedidoIdPedidoParaActualizar
 * (@Lock(PESSIMISTIC_WRITE)), equivalente Java del SELECT ... FOR UPDATE que
 * ya usan fn_seleccionar_ganadores_sorteo y fn_registrar_infraccion. La
 * aserción es sobre el INVARIANTE de negocio -- "el escrow se libera una sola
 * vez" -- no sobre el orden de ejecucion, que es no determinista.
 *
 * @SpringBootTest (no @DataJpaTest): aprobarEntrega depende de ChatService y
 * NotificacionService ademas de los repositorios JPA, asi que hace falta el
 * contexto completo. Sin @Transactional en la clase: cada hilo necesita su
 * propia transaccion real tomada del pool para que el bloqueo pesimista
 * sirva de algo (una unica transaccion de prueba compartida serializaria
 * todo por si sola y ocultaria el bug).
 *
 * Requiere Postgres real (el bloqueo de fila es comportamiento del motor, no
 * reproducible de forma fiable contra H2). Ejecutar con:
 *   ./mvnw test -Dtest=AprobarEntregaConcurrenciaIT -Dspring.profiles.active=postgres-it
 * (requiere docker compose -f artisync/docker-compose.yml up -d postgres)
 */
@Tag("integracion")
@SpringBootTest
@ActiveProfiles("postgres-it")
class AprobarEntregaConcurrenciaIT {

    private static final long ID_CLIENTE = 9201L;
    private static final long ID_CREADOR = 9202L;
    private static final int HILOS = 10;

    @Autowired
    private IEntregableServicio entregableServicio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long idPedido;
    private Long idPago;
    private Long idServicio;
    private Long idSubcategoria;
    private Long idCategoria;
    private Long idFlujo;
    private Long idEtapa;
    private Long idPerfil;

    private void sembrarDatos() {
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Cliente', 'Concurrencia', 'cliente-concurrencia-escrow@test.dev', 'x', true)",
                ID_CLIENTE);
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Creador', 'Concurrencia', 'creador-concurrencia-escrow@test.dev', 'x', true)",
                ID_CREADOR);

        idPerfil = jdbcTemplate.queryForObject(
                "INSERT INTO perfiles_creadores (id_usuario) VALUES (?) RETURNING id_perfil",
                Long.class, ID_CREADOR);

        idCategoria = jdbcTemplate.queryForObject(
                "INSERT INTO categorias (nombre_categoria) VALUES (?) RETURNING id_categoria",
                Long.class, "Categoria concurrencia escrow " + System.nanoTime());
        idSubcategoria = jdbcTemplate.queryForObject(
                "INSERT INTO subcategorias (id_categoria, nombre_subcategoria) VALUES (?, ?) RETURNING id_subcategoria",
                Long.class, idCategoria, "Subcategoria concurrencia escrow");
        idServicio = jdbcTemplate.queryForObject(
                "INSERT INTO servicios (id_perfil, id_subcategoria, titulo_servicio, descripcion_detallada, precio_base) " +
                        "VALUES (?, ?, 'Servicio concurrencia escrow', 'Descripcion de prueba', 100.00) RETURNING id_servicio",
                Long.class, idPerfil, idSubcategoria);

        idFlujo = jdbcTemplate.queryForObject(
                "INSERT INTO flujos_trabajo (nombre_flujo) VALUES (?) RETURNING id_flujo",
                Long.class, "Flujo concurrencia escrow " + System.nanoTime());
        idEtapa = jdbcTemplate.queryForObject(
                "INSERT INTO etapas_flujo (nombre_etapa) VALUES (?) RETURNING id_etapa",
                Long.class, "Etapa concurrencia escrow " + System.nanoTime());
        jdbcTemplate.update(
                "INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final) VALUES (?, ?, 1, true)",
                idFlujo, idEtapa);

        idPedido = jdbcTemplate.queryForObject(
                "INSERT INTO pedidos (id_usuario_cliente, id_servicio, id_flujo, precio_pactado) " +
                        "VALUES (?, ?, ?, 100.00) RETURNING id_pedido",
                Long.class, ID_CLIENTE, idServicio, idFlujo);

        Long idPlantilla = jdbcTemplate.queryForObject(
                "SELECT id_plantilla FROM plantillas_contrato LIMIT 1", Long.class);
        Long idContrato = jdbcTemplate.queryForObject(
                "INSERT INTO contratos (id_pedido, id_plantilla) VALUES (?, ?) RETURNING id_contrato",
                Long.class, idPedido, idPlantilla);

        jdbcTemplate.update(
                "INSERT INTO entregables_finales (id_pedido, esta_liberado) VALUES (?, false)", idPedido);

        idPago = jdbcTemplate.queryForObject(
                "INSERT INTO pagos_garantia (id_contrato, monto_retenido, estado_fondos) VALUES (?, 100.00, 'Retenido') RETURNING id_pago",
                Long.class, idContrato);
    }

    @AfterEach
    void limpiar() {
        // pedidos NO tiene ON DELETE CASCADE desde usuarios/servicios/flujos:
        // hay que borrar en el orden real de dependencia, no confiar en que
        // borrar los usuarios arrastre el resto.
        if (idPedido != null) {
            // contratos, pagos_garantia y transacciones_pago SI cascadean
            // desde pedidos (contratos.id_pedido ON DELETE CASCADE -> ...);
            // entregables_finales tambien cascadea desde pedidos.
            jdbcTemplate.update("DELETE FROM pedidos WHERE id_pedido = ?", idPedido);
        }
        if (idServicio != null) jdbcTemplate.update("DELETE FROM servicios WHERE id_servicio = ?", idServicio);
        if (idSubcategoria != null) jdbcTemplate.update("DELETE FROM subcategorias WHERE id_subcategoria = ?", idSubcategoria);
        if (idCategoria != null) jdbcTemplate.update("DELETE FROM categorias WHERE id_categoria = ?", idCategoria);
        if (idFlujo != null) jdbcTemplate.update("DELETE FROM flujos_trabajo WHERE id_flujo = ?", idFlujo);
        if (idEtapa != null) jdbcTemplate.update("DELETE FROM etapas_flujo WHERE id_etapa = ?", idEtapa);
        if (idPerfil != null) jdbcTemplate.update("DELETE FROM perfiles_creadores WHERE id_perfil = ?", idPerfil);
        jdbcTemplate.update("DELETE FROM usuarios WHERE id_usuario IN (?, ?)", ID_CLIENTE, ID_CREADOR);
    }

    @Test
    void soloUnaSolicitudLiberaElEscrow() throws Exception {
        sembrarDatos();

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listos = new CountDownLatch(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();

        for (int i = 0; i < HILOS; i++) {
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await(10, TimeUnit.SECONDS); // arranque simultaneo de los N hilos
                    entregableServicio.aprobarEntrega(idPedido, ID_CLIENTE);
                    exitos.incrementAndGet();
                } catch (Exception ignorada) {
                    // Se espera que HILOS-1 lancen ExcepcionReglaNegocio
                    // ("El entregable ya fue aprobado"): es el comportamiento
                    // correcto, no un fallo de la prueba.
                }
            });
        }

        listos.await(10, TimeUnit.SECONDS);
        salida.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // Invariante de negocio: exactamente un hilo logro aprobar la
        // entrega, sin importar cual (el orden entre hilos es no
        // determinista).
        assertThat(exitos.get()).isEqualTo(1);

        Integer egresos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transacciones_pago WHERE id_pago = ? AND tipo_transaccion = 'Egreso'",
                Integer.class, idPago);
        assertThat(egresos).isEqualTo(1);

        Integer comisiones = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transacciones_pago WHERE id_pago = ? AND tipo_transaccion = 'Comision'",
                Integer.class, idPago);
        assertThat(comisiones).isEqualTo(1);

        Boolean liberado = jdbcTemplate.queryForObject(
                "SELECT esta_liberado FROM entregables_finales WHERE id_pedido = ?",
                Boolean.class, idPedido);
        assertThat(liberado).isTrue();
    }
}
