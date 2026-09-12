package uteq.edu.ec.artisync.service.seguridad.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.service.seguridad.PrivacyService;
import uteq.edu.ec.artisync.service.seguridad.TwoFactorService;
import uteq.edu.ec.artisync.service.shared.AuthAttemptsService;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

/**
 * REQ-NF-018: prueba de integración contra PostgreSQL real, complementaria a
 * PrivacidadServiceImplTest (unitaria con Mockito, no ejercita las consultas
 * JPA derivadas reales: bloqueo pesimista de findByIdParaAnonimizar,
 * existsByFlujoIdFlujoAndEtapaIdEtapaAndEsEtapaFinalTrue, findByPedido...).
 *
 * @DataJpaTest, mismo criterio que AprobarEntregaConcurrenciaIT: TwoFactorService,
 * AuthAttemptsService, SessionRevocationService y DocumentStorage
 * no participan en las consultas JPA que se quieren ejercitar aquí, así que se
 * sustituyen por mocks en vez de levantar el contexto completo de Spring.
 *
 * Requiere Postgres real (el bloqueo de fila PESSIMISTIC_WRITE es
 * comportamiento del motor). Ejecutar con:
 *   docker compose -f artisync/docker-compose.yml up -d --wait postgres
 *   ./mvnw test -Dtest=PrivacidadServiceImplIT -Dspring.profiles.active=postgres-it
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
@Import({PrivacyServiceImpl.class, PrivacidadServiceImplIT.Colaboradores.class})
class PrivacidadServiceImplIT {

    @TestConfiguration
    static class Colaboradores {
        @Bean
        TwoFactorService twoFactorService() {
            return Mockito.mock(TwoFactorService.class);
        }

        @Bean
        AuthAttemptsService intentosAutenticacionService() {
            return Mockito.mock(AuthAttemptsService.class);
        }

        @Bean
        SessionRevocationService sessionRevocationService() {
            return Mockito.mock(SessionRevocationService.class);
        }

        @Bean
        DocumentStorage almacenamientoDocumentos() {
            return Mockito.mock(DocumentStorage.class);
        }
    }

    private static final long ID_ADMIN = 9301L;
    private static final long ID_CLIENTE = 9302L;
    private static final long ID_CREADOR = 9303L;

    @Autowired
    private PrivacyService privacidadService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SessionRevocationService sessionRevocationService;

    @Autowired
    private TestEntityManager entityManager;

    private Long idPedido;
    private Long idServicio;
    private Long idSubcategoria;
    private Long idCategoria;
    private Long idFlujo;
    private Long idEtapa;
    private Long idPerfil;

    // El contexto @DataJpaTest se cachea entre los 4 tests de esta clase, asi
    // que los mocks (beans singleton) acumulan invocaciones de un test a
    // otro si no se resetean.
    @BeforeEach
    void resetMocks() {
        Mockito.reset(sessionRevocationService);
    }

    /**
     * Siembra un creador (sujeto de la anonimización) con perfil, servicio,
     * flujo de una sola etapa, un pedido de un cliente sobre ese servicio,
     * un certificado IA y datos de pago propios.
     *
     * @param pedidoEnEtapaFinal si es false, no inserta fila en
     *                           historial_estados_pedido (caso "sin
     *                           transición = en curso" del código real).
     * @param fondosRetenidos    si es true, el pago en garantía del contrato
     *                           queda en estado_fondos = 'Retenido'.
     */
    private void sembrarDatos(boolean pedidoEnEtapaFinal, boolean fondosRetenidos) {
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Admin', 'PrivacidadIT', 'admin-privacidad-it@test.dev', 'x', true)",
                ID_ADMIN);
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Cliente', 'PrivacidadIT', 'cliente-privacidad-it@test.dev', 'x', true)",
                ID_CLIENTE);
        jdbcTemplate.update(
                "INSERT INTO usuarios (id_usuario, nombres, apellidos, correo, contrasena_hash, estado_cuenta) " +
                        "VALUES (?, 'Creador', 'PrivacidadIT', 'creador-privacidad-it@test.dev', 'x', true)",
                ID_CREADOR);

        idPerfil = jdbcTemplate.queryForObject(
                "INSERT INTO perfiles_creadores (id_usuario) VALUES (?) RETURNING id_perfil",
                Long.class, ID_CREADOR);

        idCategoria = jdbcTemplate.queryForObject(
                "INSERT INTO categorias (nombre_categoria) VALUES (?) RETURNING id_categoria",
                Long.class, "Category privacidad IT " + System.nanoTime());
        idSubcategoria = jdbcTemplate.queryForObject(
                "INSERT INTO subcategorias (id_categoria, nombre_subcategoria) VALUES (?, ?) RETURNING id_subcategoria",
                Long.class, idCategoria, "Subcategory privacidad IT");
        idServicio = jdbcTemplate.queryForObject(
                "INSERT INTO servicios (id_perfil, titulo_servicio, descripcion_detallada, precio_base) " +
                        "VALUES (?, 'Offering privacidad IT', 'Descripcion de prueba', 50.00) RETURNING id_servicio",
                Long.class, idPerfil);
        jdbcTemplate.update(
                "INSERT INTO servicio_subcategorias (id_servicio, id_subcategoria) VALUES (?, ?)",
                idServicio, idSubcategoria);

        idFlujo = jdbcTemplate.queryForObject(
                "INSERT INTO flujos_trabajo (nombre_flujo, id_usuario_creador) VALUES (?, ?) RETURNING id_flujo",
                Long.class, "Flujo privacidad IT " + System.nanoTime(), ID_CREADOR);
        idEtapa = jdbcTemplate.queryForObject(
                "INSERT INTO etapas_flujo (nombre_etapa) VALUES (?) RETURNING id_etapa",
                Long.class, "Etapa privacidad IT " + System.nanoTime());
        jdbcTemplate.update(
                "INSERT INTO flujo_etapas_config (id_flujo, id_etapa, numero_orden, es_etapa_final) VALUES (?, ?, 1, true)",
                idFlujo, idEtapa);

        idPedido = jdbcTemplate.queryForObject(
                "INSERT INTO pedidos (id_usuario_cliente, id_servicio, id_flujo, precio_pactado) " +
                        "VALUES (?, ?, ?, 50.00) RETURNING id_pedido",
                Long.class, ID_CLIENTE, idServicio, idFlujo);

        if (pedidoEnEtapaFinal) {
            jdbcTemplate.update(
                    "INSERT INTO historial_estados_pedido (id_pedido, id_etapa) VALUES (?, ?)",
                    idPedido, idEtapa);
        }

        Long idPlantilla = jdbcTemplate.queryForObject(
                "SELECT id_plantilla FROM plantillas_contrato LIMIT 1", Long.class);
        Long idContrato = jdbcTemplate.queryForObject(
                "INSERT INTO contratos (id_pedido, id_plantilla) VALUES (?, ?) RETURNING id_contrato",
                Long.class, idPedido, idPlantilla);
        jdbcTemplate.update(
                "INSERT INTO pagos_garantia (id_contrato, monto_retenido, estado_fondos) VALUES (?, 50.00, ?)",
                idContrato, fondosRetenidos ? "Retenido" : "Liberado");

        jdbcTemplate.update(
                "INSERT INTO datos_pago_creador (id_usuario, correo_paypal) VALUES (?, 'creador-privacidad-it@paypal.test')",
                ID_CREADOR);

        Long idEstadoPendiente = jdbcTemplate.queryForObject(
                "SELECT id_estado_verificacion FROM estados_verificacion WHERE nombre_estado = 'PENDIENTE'", Long.class);
        jdbcTemplate.update(
                "INSERT INTO certificados_ia (id_usuario, id_estado_verificacion, url_documento_s3, datos_extraidos_ia, hash_documento) " +
                        "VALUES (?, ?, 'https://s3.test/doc.png', '{\"nombre\":\"dato sensible\"}', 'hash-original')",
                ID_CREADOR, idEstadoPendiente);
    }

    @AfterEach
    void limpiar() {
        // Mismo orden de dependencia real que AprobarEntregaConcurrenciaIT:
        // pedidos NO tiene ON DELETE CASCADE desde servicios/flujos.
        if (idPedido != null) {
            jdbcTemplate.update("DELETE FROM pedidos WHERE id_pedido = ?", idPedido);
        }
        if (idServicio != null) jdbcTemplate.update("DELETE FROM servicios WHERE id_servicio = ?", idServicio);
        if (idSubcategoria != null) jdbcTemplate.update("DELETE FROM subcategorias WHERE id_subcategoria = ?", idSubcategoria);
        if (idCategoria != null) jdbcTemplate.update("DELETE FROM categorias WHERE id_categoria = ?", idCategoria);
        if (idFlujo != null) jdbcTemplate.update("DELETE FROM flujos_trabajo WHERE id_flujo = ?", idFlujo);
        if (idEtapa != null) jdbcTemplate.update("DELETE FROM etapas_flujo WHERE id_etapa = ?", idEtapa);
        if (idPerfil != null) jdbcTemplate.update("DELETE FROM perfiles_creadores WHERE id_perfil = ?", idPerfil);
        // usuarios cascadea datos_pago_creador y certificados_ia (ON DELETE CASCADE).
        jdbcTemplate.update("DELETE FROM usuarios WHERE id_usuario IN (?, ?, ?)", ID_ADMIN, ID_CLIENTE, ID_CREADOR);
    }

    @Test
    void anonimizarUsuarioAdmin_anonimizaTodo_sinPedidoEnCursoNiFondosRetenidos() {
        sembrarDatos(true, false);

        privacidadService.anonymizeUserAsAdmin(ID_CREADOR, ID_ADMIN);
        entityManager.flush(); // sincroniza los UPDATE de Hibernate antes de leer con JdbcTemplate

        String correo = jdbcTemplate.queryForObject(
                "SELECT correo FROM usuarios WHERE id_usuario = ?", String.class, ID_CREADOR);
        assertThat(correo).endsWith("@eliminado.artisync.invalid");

        // sessionRevocationService esta mockeado (no participa en las
        // consultas JPA que se ejercitan aqui): el efecto real de desactivar
        // la cuenta lo cubre SessionRevocationServiceTest por separado; aqui
        // solo se verifica que PrivacyServiceImpl lo invoca correctamente.
        verify(sessionRevocationService).cambiarEstadoCuenta(ID_CREADOR, false);

        String correoPaypal = jdbcTemplate.queryForObject(
                "SELECT correo_paypal FROM datos_pago_creador WHERE id_usuario = ?", String.class, ID_CREADOR);
        assertThat(correoPaypal).isEqualTo("eliminado@eliminado.artisync.invalid");

        String datosExtraidos = jdbcTemplate.queryForObject(
                "SELECT datos_extraidos_ia FROM certificados_ia WHERE id_usuario = ?", String.class, ID_CREADOR);
        assertThat(datosExtraidos).isNull();
    }

    @Test
    void anonimizarUsuarioAdmin_rechaza_siHayPedidoSinTransicionRegistrada() {
        sembrarDatos(false, false);

        assertThatThrownBy(() -> privacidadService.anonymizeUserAsAdmin(ID_CREADOR, ID_ADMIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pedido en curso");

        String correo = jdbcTemplate.queryForObject(
                "SELECT correo FROM usuarios WHERE id_usuario = ?", String.class, ID_CREADOR);
        assertThat(correo).doesNotContain("@eliminado.artisync.invalid");
    }

    @Test
    void anonimizarUsuarioAdmin_declaraExcepcionLegal_conFondosRetenidos() {
        sembrarDatos(true, true);

        var respuesta = privacidadService.anonymizeUserAsAdmin(ID_CREADOR, ID_ADMIN);
        entityManager.flush();

        assertThat(respuesta.getMensaje()).contains("excepciones legales");

        // usuarios y certificados_ia SI se anonimizan; solo datos_pago_creador se conserva.
        String correo = jdbcTemplate.queryForObject(
                "SELECT correo FROM usuarios WHERE id_usuario = ?", String.class, ID_CREADOR);
        assertThat(correo).endsWith("@eliminado.artisync.invalid");

        String correoPaypal = jdbcTemplate.queryForObject(
                "SELECT correo_paypal FROM datos_pago_creador WHERE id_usuario = ?", String.class, ID_CREADOR);
        assertThat(correoPaypal).isEqualTo("creador-privacidad-it@paypal.test");
    }

    @Test
    void solicitarSupresionPropia_esIdempotente_contraElBloqueoPesimistaReal() {
        sembrarDatos(true, false);

        privacidadService.requestOwnErasure(ID_CREADOR, null);
        var segundaRespuesta = privacidadService.requestOwnErasure(ID_CREADOR, null);

        assertThat(segundaRespuesta.getMensaje())
                .isEqualTo("Tus datos personales ya fueron suprimidos anteriormente.");
    }
}
