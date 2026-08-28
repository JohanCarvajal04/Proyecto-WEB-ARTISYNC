package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionResponderBriefing;
import uteq.edu.ec.artisync.entity.catalogo.Categoria;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.catalogo.Subcategoria;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingEnviado;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingPlantilla;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.catalogo.CategoriaRepository;
import uteq.edu.ec.artisync.repository.catalogo.FlujoTrabajoRepository;
import uteq.edu.ec.artisync.repository.catalogo.ServicioRepository;
import uteq.edu.ec.artisync.repository.catalogo.SubcategoriaRepository;
import uteq.edu.ec.artisync.repository.comunicacion.BriefingEnviadoRepository;
import uteq.edu.ec.artisync.repository.comunicacion.BriefingPlantillaRepository;
import uteq.edu.ec.artisync.repository.comunicacion.BriefingRespuestaRepository;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.pedido.PlantillaContratoRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.comunicacion.BriefingService;
import uteq.edu.ec.artisync.service.shared.EmailService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Hallazgo 1.1 del INFORME-REVISION-COMPLETA.md: reproduce contra los proxies
 * transaccionales reales de Spring el {@code UnexpectedRollbackException} que
 * perdía silenciosamente las respuestas del briefing cuando el pedido ya tenía
 * un contrato generado (ver comentarios en {@link BriefingServiceImpl#responderBriefing}
 * y {@link uteq.edu.ec.artisync.service.legal.impl.ContratoServicioImpl#generarContrato}).
 *
 * A propósito {@code @SpringBootTest} (no Mockito): el bug solo existe a
 * través del interceptor AOP transaccional real — los tests con
 * {@code @Mock IContratoServicio} de {@link BriefingServiceImplTest} nunca lo
 * habrían detectado, porque ahí la excepción no pasa por ningún proxy
 * transaccional.
 *
 * Deliberadamente SIN {@code @Transactional} de clase: envolver el test en una
 * transacción ocultaría el propio {@code UnexpectedRollbackException} que se
 * quiere observar al confirmar. Limpieza manual en {@link #limpiar()}.
 *
 * Para comprobar que este test realmente detecta la regresión: revertir
 * temporalmente {@code ContratoServicioImpl.generarContrato} de
 * {@code Propagation.REQUIRES_NEW} a {@code @Transactional} (por defecto,
 * REQUIRED) y volver a correr solo esta clase — debe fallar con
 * {@code UnexpectedRollbackException}.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "JWT_SECRET=f98cf546c1a89c93f0b2f1559868779b76c8c4a4f89d0b676a74c431d1d8ef3f"
})
class BriefingContratoTransaccionTest {

    @MockitoBean
    private EmailService emailService;

    @Autowired private BriefingService briefingService;

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PerfilCreadorRepository perfilCreadorRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private SubcategoriaRepository subcategoriaRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private FlujoTrabajoRepository flujoTrabajoRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private PlantillaContratoRepository plantillaContratoRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private BriefingPlantillaRepository briefingPlantillaRepository;
    @Autowired private BriefingEnviadoRepository briefingEnviadoRepository;
    @Autowired private BriefingRespuestaRepository briefingRespuestaRepository;

    private Long idPedido;
    private Long idCliente;
    private Long idBriefingEnviado;

    @BeforeEach
    void sembrarDatos() {
        Usuario cliente = usuarioRepository.save(Usuario.builder()
                .nombres("Cliente").apellidos("Prueba")
                .correo("cliente.transaccion." + System.nanoTime() + "@test.dev")
                .contrasenaHash("x").build());
        Usuario creador = usuarioRepository.save(Usuario.builder()
                .nombres("Creador").apellidos("Prueba")
                .correo("creador.transaccion." + System.nanoTime() + "@test.dev")
                .contrasenaHash("x").build());
        idCliente = cliente.getIdUsuario();

        PerfilCreador perfil = perfilCreadorRepository.save(
                PerfilCreador.builder().usuario(creador).build());

        Categoria categoria = categoriaRepository.save(
                Categoria.builder().nombreCategoria("Categoria transaccion " + System.nanoTime()).build());
        Subcategoria subcategoria = subcategoriaRepository.save(
                Subcategoria.builder().categoria(categoria).nombreSubcategoria("Subcategoria").build());
        Servicio servicio = servicioRepository.save(Servicio.builder()
                .perfil(perfil).subcategoria(subcategoria)
                .tituloServicio("Servicio de prueba")
                .descripcionDetallada("Descripcion suficientemente larga para pasar la validacion.")
                .precioBase(new BigDecimal("50.00"))
                .build());
        FlujoTrabajo flujo = flujoTrabajoRepository.save(
                FlujoTrabajo.builder().nombreFlujo("Flujo transaccion").build());

        Pedido pedido = pedidoRepository.save(Pedido.builder()
                .usuarioCliente(cliente).servicio(servicio).flujo(flujo)
                .precioPactado(new BigDecimal("50.00"))
                .build());
        idPedido = pedido.getIdPedido();

        PlantillaContrato plantillaContrato = plantillaContratoRepository.save(PlantillaContrato.builder()
                .versionLegal("v-transaccion-" + System.nanoTime())
                .cuerpoHtmlPlantilla("<html>Contrato de prueba</html>")
                .build());

        // Condición que dispara ExcepcionReglaNegocio dentro de generarContrato:
        // el pedido YA tiene un contrato antes de responder el briefing.
        contratoRepository.save(Contrato.builder()
                .pedido(pedido).plantilla(plantillaContrato).build());

        BriefingPlantilla plantillaBriefing = briefingPlantillaRepository.save(BriefingPlantilla.builder()
                .perfilCreador(perfil).nombrePlantilla("Briefing de prueba").build());

        BriefingEnviado enviado = briefingEnviadoRepository.save(BriefingEnviado.builder()
                .pedido(pedido).plantilla(plantillaBriefing).completado(false).build());
        idBriefingEnviado = enviado.getIdBriefingEnviado();
    }

    @AfterEach
    void limpiar() {
        briefingRespuestaRepository.deleteAll(
                briefingRespuestaRepository.findByBriefingEnviadoIdBriefingEnviado(idBriefingEnviado));
        briefingEnviadoRepository.deleteById(idBriefingEnviado);
        briefingPlantillaRepository.deleteAll();
        contratoRepository.deleteAll();
        plantillaContratoRepository.deleteAll();
        pedidoRepository.deleteById(idPedido);
        servicioRepository.deleteAll();
        subcategoriaRepository.deleteAll();
        categoriaRepository.deleteAll();
        flujoTrabajoRepository.deleteAll();
        perfilCreadorRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("responderBriefing no pierde las respuestas cuando el pedido ya tiene contrato (H-1.1)")
    void responderBriefing_pedidoConContratoExistente_persisteRespuestas() {
        PeticionResponderBriefing peticion =
                PeticionResponderBriefing.builder().respuestas(List.of()).build();

        // Antes del fix: UnexpectedRollbackException al confirmar, porque
        // generarContrato (misma transacción física) marca rollbackOnly al
        // lanzar ExcepcionReglaNegocio ("Ya existe un contrato...").
        assertDoesNotThrow(() -> briefingService.responderBriefing(idPedido, peticion, idCliente));

        // La transacción de responderBriefing ya confirmó al volver del
        // método (no hay @Transactional envolviendo el test) — releer desde
        // el repositorio confirma que quedó realmente persistido, no solo en
        // el objeto en memoria.
        BriefingEnviado persistido = briefingEnviadoRepository.findById(idBriefingEnviado).orElseThrow();
        assertThat(persistido.getCompletado()).isTrue();
    }
}
