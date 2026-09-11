package uteq.edu.ec.artisync.service.comunicacion.impl;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.SendMessageRequest;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.ChatMessageResponse;
import uteq.edu.ec.artisync.security.CustomUserDetailsService;
import uteq.edu.ec.artisync.security.JwtService;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REQ-NF-005: abre ≥10 conexiones STOMP/WebSocket simultáneas sobre el chat
 * real y mide la latencia extremo-a-extremo (envío → recepción en cada
 * suscriptor) contra el umbral de ≤500ms declarado en el SRS. Reemplaza la
 * medición manual con un cliente STOMP externo por una prueba re-ejecutable:
 * basta con correr esta clase para reverificar el umbral.
 *
 * @SpringBootTest con puerto aleatorio real (no @DataJpaTest): STOMP necesita
 * un servidor HTTP/WebSocket real escuchando, y la autenticación real del
 * CONNECT pasa por CustomUserDetailsService, que depende de la función
 * PL/pgSQL fn_permisos_efectivos_usuario — no ejecutable contra H2.
 *
 * Requiere Postgres real. Ejecutar con:
 *   docker compose -f artisync/docker-compose.yml up -d --wait postgres
 *   ./mvnw test -Dtest=ChatWebSocketLoadIT -Dspring.profiles.active=postgres-it \
 *     -DDB_NAME=artisyncbd -DDB_USER=postgres -DDB_PASSWORD=changeme
 * (ajustar las 3 últimas propiedades a las credenciales reales de tu
 * artisync/.env si difieren de los valores por defecto del perfil).
 */
@Tag("integracion")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres-it")
// Deshabilitado en CI (GitHub Actions): las máquinas compartidas pueden superar
// el umbral de 500ms de latencia WebSocket, causando fallos intermitentes.
// Adicionalmente, esta prueba requiere Redis real corriendo, servicio que no
// está disponible en el pipeline de integración (solo se levanta Postgres).
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class ChatWebSocketLoadIT {

    private static final long ID_CLIENTE = 9501L;
    private static final long ID_CREADOR = 9502L;
    private static final int NUMERO_CONEXIONES = 10;
    private static final int NUMERO_RONDAS = 5;
    private static final long UMBRAL_LATENCIA_MS = 500L;

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private Long idPedido;
    private Long idServicio;
    private Long idSubcategoria;
    private Long idCategoria;
    private Long idFlujo;
    private Long idEtapa;
    private Long idPerfil;
    private Long idSala;

    private void sembrarPedidoConSala() {
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Cliente', 'WsLoadIT', 'cliente-ws-load-it@test.dev', 'x', true)",
                ID_CLIENTE);
        jdbcTemplate.update(
                "INSERT INTO usuario_roles (id_usuario, id_rol) SELECT ?, id_rol FROM roles WHERE nombre_rol = 'CLIENTE'",
                ID_CLIENTE);
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Creador', 'WsLoadIT', 'creador-ws-load-it@test.dev', 'x', true)",
                ID_CREADOR);

        idPerfil = jdbcTemplate.queryForObject(
                "INSERT INTO perfiles_creadores (id_usuario) VALUES (?) RETURNING id_perfil",
                Long.class, ID_CREADOR);

        idCategoria = jdbcTemplate.queryForObject(
                "INSERT INTO categorias (nombre_categoria) VALUES (?) RETURNING id_categoria",
                Long.class, "Category WS load IT " + System.nanoTime());
        idSubcategoria = jdbcTemplate.queryForObject(
                "INSERT INTO subcategorias (id_categoria, nombre_subcategoria) VALUES (?, ?) RETURNING id_subcategoria",
                Long.class, idCategoria, "Subcategory WS load IT");
        idServicio = jdbcTemplate.queryForObject(
                "INSERT INTO servicios (id_perfil, titulo_servicio, descripcion_detallada, precio_base) " +
                        "VALUES (?, 'Offering WS load IT', 'Descripcion de prueba', 50.00) RETURNING id_servicio",
                Long.class, idPerfil);
        jdbcTemplate.update(
                "INSERT INTO servicio_subcategorias (id_servicio, id_subcategoria) VALUES (?, ?)",
                idServicio, idSubcategoria);

        idFlujo = jdbcTemplate.queryForObject(
                "INSERT INTO flujos_trabajo (nombre_flujo, id_usuario_creador) VALUES (?, ?) RETURNING id_flujo",
                Long.class, "Flujo WS load IT " + System.nanoTime(), ID_CREADOR);
        idEtapa = jdbcTemplate.queryForObject(
                "INSERT INTO etapas_flujo (nombre_etapa) VALUES (?) RETURNING id_etapa",
                Long.class, "Etapa WS load IT " + System.nanoTime());
        jdbcTemplate.update(
                "INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final) VALUES (?, ?, 1, true)",
                idFlujo, idEtapa);

        idPedido = jdbcTemplate.queryForObject(
                "INSERT INTO pedidos (id_usuario_cliente, id_servicio, id_flujo, precio_pactado) " +
                        "VALUES (?, ?, ?, 50.00) RETURNING id_pedido",
                Long.class, ID_CLIENTE, idServicio, idFlujo);

        idSala = jdbcTemplate.queryForObject(
                "INSERT INTO salas_chat (id_pedido, sala_activa) VALUES (?, true) RETURNING id_sala",
                Long.class, idPedido);
    }

    @AfterEach
    void limpiar() {
        if (idSala != null) jdbcTemplate.update("DELETE FROM mensajes WHERE id_sala = ?", idSala);
        if (idSala != null) jdbcTemplate.update("DELETE FROM salas_chat WHERE id_sala = ?", idSala);
        if (idPedido != null) jdbcTemplate.update("DELETE FROM pedidos WHERE id_pedido = ?", idPedido);
        if (idServicio != null) jdbcTemplate.update("DELETE FROM servicios WHERE id_servicio = ?", idServicio);
        if (idSubcategoria != null) jdbcTemplate.update("DELETE FROM subcategorias WHERE id_subcategoria = ?", idSubcategoria);
        if (idCategoria != null) jdbcTemplate.update("DELETE FROM categorias WHERE id_categoria = ?", idCategoria);
        if (idFlujo != null) jdbcTemplate.update("DELETE FROM flujos_trabajo WHERE id_flujo = ?", idFlujo);
        if (idEtapa != null) jdbcTemplate.update("DELETE FROM etapas_flujo WHERE id_etapa = ?", idEtapa);
        if (idPerfil != null) jdbcTemplate.update("DELETE FROM perfiles_creadores WHERE id_perfil = ?", idPerfil);
        jdbcTemplate.update("DELETE FROM usuarios WHERE id_usuario IN (?, ?)", ID_CLIENTE, ID_CREADOR);
    }

    @Test
    void diezConexionesSimultaneas_latenciaExtremoAExtremo_bajoElUmbralDe500ms() throws Exception {
        sembrarPedidoConSala();

        UserDetails userDetails = userDetailsService.loadUserByUsername("cliente-ws-load-it@test.dev");
        String token = jwtService.generarToken(userDetails);

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.getObjectMapper().registerModule(new JavaTimeModule());
        stompClient.setMessageConverter(converter);

        List<Long> latenciasMs = new CopyOnWriteArrayList<>();
        AtomicReference<String> nonceEsperado = new AtomicReference<>("");
        AtomicReference<Long> enviadoEnNanos = new AtomicReference<>(0L);
        AtomicReference<CountDownLatch> latchRonda = new AtomicReference<>();

        List<StompSession> sesiones = new java.util.ArrayList<>();
        String url = "ws://localhost:" + port + "/ws/websocket";

        try {
            for (int i = 0; i < NUMERO_CONEXIONES; i++) {
                StompHeaders connectHeaders = new StompHeaders();
                connectHeaders.add("Authorization", "Bearer " + token);

                StompSession session = stompClient
                        .connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                        .get(5, TimeUnit.SECONDS);
                sesiones.add(session);

                session.subscribe("/topic/sala." + idSala, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return ChatMessageResponse.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        long recibidoEnNanos = System.nanoTime();
                        ChatMessageResponse mensaje = (ChatMessageResponse) payload;
                        if (mensaje != null && nonceEsperado.get().equals(mensaje.getCuerpoMensaje())) {
                            long latenciaMs = (recibidoEnNanos - enviadoEnNanos.get()) / 1_000_000;
                            latenciasMs.add(latenciaMs);
                            CountDownLatch latch = latchRonda.get();
                            if (latch != null) latch.countDown();
                        }
                    }
                });
            }

            assertThat(sesiones).hasSize(NUMERO_CONEXIONES);
            sesiones.forEach(s -> assertThat(s.isConnected()).isTrue());

            for (int ronda = 1; ronda <= NUMERO_RONDAS; ronda++) {
                String nonce = "carga-ws-it-ronda" + ronda + "-" + UUID.randomUUID();
                nonceEsperado.set(nonce);
                CountDownLatch latch = new CountDownLatch(NUMERO_CONEXIONES);
                latchRonda.set(latch);

                SendMessageRequest peticion = SendMessageRequest.builder()
                        .idPedido(idPedido)
                        .cuerpoMensaje(nonce)
                        .build();

                enviadoEnNanos.set(System.nanoTime());
                sesiones.get(0).send("/app/chat.enviar", peticion);

                boolean recibidoPorTodos = latch.await(5, TimeUnit.SECONDS);
                assertThat(recibidoPorTodos)
                        .as("Ronda %d: las %d conexiones deben recibir el broadcast en <=5s (recibidas: %d)",
                                ronda, NUMERO_CONEXIONES, NUMERO_CONEXIONES - latch.getCount())
                        .isTrue();
            }
        } finally {
            sesiones.forEach(s -> {
                try {
                    if (s.isConnected()) s.disconnect();
                } catch (Exception ignored) {
                }
            });
        }

        assertThat(latenciasMs).hasSize(NUMERO_CONEXIONES * NUMERO_RONDAS);

        List<Long> ordenadas = latenciasMs.stream().sorted().collect(Collectors.toList());
        long p50 = ordenadas.get(ordenadas.size() / 2);
        long p95 = ordenadas.get((int) Math.min(ordenadas.size() - 1, Math.ceil(ordenadas.size() * 0.95) - 1));
        long maximo = ordenadas.get(ordenadas.size() - 1);
        double media = ordenadas.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.printf("[REQ-NF-005] %d conexiones x %d rondas = %d muestras. " +
                        "media=%.1fms p50=%dms p95=%dms max=%dms (umbral %dms)%n",
                NUMERO_CONEXIONES, NUMERO_RONDAS, latenciasMs.size(), media, p50, p95, maximo, UMBRAL_LATENCIA_MS);

        assertThat(p95)
                .as("p95 de latencia extremo-a-extremo con %d conexiones simultáneas debe ser <= %d ms",
                        NUMERO_CONEXIONES, UMBRAL_LATENCIA_MS)
                .isLessThanOrEqualTo(UMBRAL_LATENCIA_MS);
    }
}
