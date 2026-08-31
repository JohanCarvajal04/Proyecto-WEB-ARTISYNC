package uteq.edu.ec.artisync.service.pedido.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionAvanzarEtapa;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPedido;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPropuestaTerminos;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaContrato;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaHistorialEstado;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedidoResumido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPropuestaTerminos;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaSeguimientoPedido;
import uteq.edu.ec.artisync.entity.catalogo.Categoria;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.catalogo.Subcategoria;
import uteq.edu.ec.artisync.entity.pedido.EtapaFlujo;
import uteq.edu.ec.artisync.entity.pedido.FlujoEtapaConfig;
import uteq.edu.ec.artisync.entity.pedido.HistorialEstadoPedido;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.PropuestaTerminosPedido;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.legal.EntregableFinalRepository;
import uteq.edu.ec.artisync.repository.pedido.PropuestaTerminosPedidoRepository;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;
import uteq.edu.ec.artisync.service.legal.IContratoServicio;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.FlujoTrabajoRepository;
import uteq.edu.ec.artisync.repository.catalogo.ServicioRepository;
import uteq.edu.ec.artisync.repository.pedido.EtapaFlujoRepository;
import uteq.edu.ec.artisync.repository.pedido.FlujoEtapaConfigRepository;
import uteq.edu.ec.artisync.repository.pedido.HistorialEstadoPedidoRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.perfil.IVerificacionServicio;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.IServicioExportacion;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pruebas unitarias de {@link PedidoServicioImpl} complementarias a
 * {@link PedidoServicioImplFlujoTest} (que solo cubre la resolución del flujo
 * por categoría, RF-19): aquí se cubren el resto de operaciones — obtener,
 * listar, avanzar etapa, historial y seguimiento — junto con el control IDOR
 * de {@code obtenerPedidoPorId} (OBS-08).
 */
@ExtendWith(MockitoExtension.class)
class PedidoServicioImplTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private FlujoTrabajoRepository flujoTrabajoRepository;
    @Mock private FlujoEtapaConfigRepository flujoEtapaConfigRepository;
    @Mock private HistorialEstadoPedidoRepository historialRepository;
    @Mock private EtapaFlujoRepository etapaFlujoRepository;
    @Mock private ContratoRepository contratoRepository;
    @Mock private EntregableFinalRepository entregableFinalRepository;
    @Mock private PropuestaTerminosPedidoRepository propuestaTerminosPedidoRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private ChatService chatService;
    @Mock private IServicioExportacion servicioExportacion;
    @Mock private IVerificacionServicio verificacionServicio;
    @Mock private IContratoServicio contratoServicio;

    @InjectMocks
    private PedidoServicioImpl pedidoServicio;

    private Usuario cliente;
    private Usuario creador;
    private Categoria categoria;
    private FlujoTrabajo flujo;
    private Servicio servicio;
    private Pedido pedido;
    private EtapaFlujo etapaInicial;
    private EtapaFlujo etapaSiguiente;

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder().idUsuario(1L).nombres("Cliente").apellidos("Uno").build();
        creador = Usuario.builder().idUsuario(2L).nombres("Creador").apellidos("Uno").build();
        flujo = FlujoTrabajo.builder().idFlujo(1L).nombreFlujo("Flujo estandar").build();
        categoria = Categoria.builder().idCategoria(1L).nombreCategoria("Arte").flujo(flujo).build();
        Subcategoria subcategoria = Subcategoria.builder().idSubcategoria(1L).categoria(categoria).build();
        PerfilCreador perfil = PerfilCreador.builder().idPerfil(1L).usuario(creador).build();
        servicio = Servicio.builder().idServicio(1L).perfil(perfil).subcategoria(subcategoria)
                .tituloServicio("Ilustracion").precioBase(new BigDecimal("20.00")).build();
        pedido = Pedido.builder().idPedido(10L).usuarioCliente(cliente).servicio(servicio)
                .flujo(flujo).precioPactado(new BigDecimal("20.00")).build();
        etapaInicial = EtapaFlujo.builder().idEtapa(1L).nombreEtapa("Inicio").build();
        etapaSiguiente = EtapaFlujo.builder().idEtapa(2L).nombreEtapa("Revision").build();

        // Por defecto el cliente ya tiene su identidad verificada: la mayoría
        // de estos tests no ejercitan el gating de REQ-F-006 ampliado.
        lenient().when(verificacionServicio.estaIdentidadVerificada(anyLong())).thenReturn(true);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ---------- crearPedido ----------

    @Test
    @DisplayName("crearPedido rechaza si el cliente es el mismo creador del servicio")
    void crearPedido_rechazaAutoPedido() {
        PeticionCrearPedido peticion = PeticionCrearPedido.builder().idServicio(1L).build();
        given(usuarioRepository.findById(2L)).willReturn(Optional.of(creador));
        given(servicioRepository.findById(1L)).willReturn(Optional.of(servicio));

        assertThatThrownBy(() -> pedidoServicio.crearPedido(2L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("crearPedido rechaza si el flujo no tiene etapas configuradas")
    void crearPedido_rechazaFlujoSinEtapas() {
        PeticionCrearPedido peticion = PeticionCrearPedido.builder().idServicio(1L).build();
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(cliente));
        given(servicioRepository.findById(1L)).willReturn(Optional.of(servicio));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        assertThatThrownBy(() -> pedidoServicio.crearPedido(1L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("crearPedido usa el precio base cuando no se ofrece un precio")
    void crearPedido_usaPrecioBase() {
        PeticionCrearPedido peticion = PeticionCrearPedido.builder().idServicio(1L).build();
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial).numeroOrden(1).build();

        given(usuarioRepository.findById(1L)).willReturn(Optional.of(cliente));
        given(servicioRepository.findById(1L)).willReturn(Optional.of(servicio));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(config));
        given(pedidoRepository.save(any(Pedido.class))).willAnswer(inv -> inv.getArgument(0));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(any())).willReturn(List.of());

        RespuestaPedido respuesta = pedidoServicio.crearPedido(1L, peticion);

        assertThat(respuesta.getPrecioPactado()).isEqualByComparingTo("20.00");
        // La sala se abre desde la creación, no al firmar: así pueden
        // negociar por chat antes de comprometerse con el contrato.
        verify(chatService).crearSala(any(Pedido.class));
    }

    @Test
    @DisplayName("crearPedido lanza recurso no encontrado si el cliente no existe")
    void crearPedido_clienteInexistente() {
        given(usuarioRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.crearPedido(1L, PeticionCrearPedido.builder().idServicio(1L).build()))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("crearPedido rechaza si el cliente no tiene la identidad verificada")
    void crearPedido_identidadNoVerificada_lanzaExcepcionReglaNegocio() {
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(cliente));
        given(verificacionServicio.estaIdentidadVerificada(1L)).willReturn(false);

        assertThatThrownBy(() -> pedidoServicio.crearPedido(1L, PeticionCrearPedido.builder().idServicio(1L).build()))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("verificar tu identidad");
        verifyNoInteractions(servicioRepository);
    }

    // ---------- proponerTerminos / aceptarPropuestaTerminos / rechazarPropuestaTerminos ----------

    @Test
    @DisplayName("proponerTerminos rechaza si no llega ningun campo")
    void proponerTerminos_rechazaPeticionVacia() {
        assertThatThrownBy(() -> pedidoServicio.proponerTerminos(
                10L, 1L, PeticionCrearPropuestaTerminos.builder().build()))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("proponerTerminos rechaza a un usuario que no es cliente ni creador del pedido")
    void proponerTerminos_rechazaUsuarioAjeno() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        PeticionCrearPropuestaTerminos peticion =
                PeticionCrearPropuestaTerminos.builder().precioPropuesto(new BigDecimal("35.00")).build();

        assertThatThrownBy(() -> pedidoServicio.proponerTerminos(10L, 999L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("proponerTerminos rechaza si ya existe una propuesta pendiente")
    void proponerTerminos_rechazaSiYaHayPropuestaPendiente() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(10L)).willReturn(Optional.empty());
        given(propuestaTerminosPedidoRepository.findByPedidoIdPedidoAndEstado(10L, PropuestaTerminosPedido.PENDIENTE))
                .willReturn(Optional.of(PropuestaTerminosPedido.builder().idPropuesta(1L).build()));
        PeticionCrearPropuestaTerminos peticion =
                PeticionCrearPropuestaTerminos.builder().precioPropuesto(new BigDecimal("35.00")).build();

        assertThatThrownBy(() -> pedidoServicio.proponerTerminos(10L, 1L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);

        verify(propuestaTerminosPedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("proponerTerminos guarda la propuesta y notifica al creador cuando propone el cliente")
    void proponerTerminos_clientePuedeProponerYNotificaCreador() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(10L)).willReturn(Optional.empty());
        given(propuestaTerminosPedidoRepository.findByPedidoIdPedidoAndEstado(10L, PropuestaTerminosPedido.PENDIENTE))
                .willReturn(Optional.empty());
        given(propuestaTerminosPedidoRepository.save(any(PropuestaTerminosPedido.class)))
                .willAnswer(inv -> inv.getArgument(0));
        PeticionCrearPropuestaTerminos peticion =
                PeticionCrearPropuestaTerminos.builder().precioPropuesto(new BigDecimal("35.00")).build();

        RespuestaPropuestaTerminos respuesta = pedidoServicio.proponerTerminos(10L, 1L, peticion);

        assertThat(respuesta.getPrecioPropuesto()).isEqualByComparingTo("35.00");
        assertThat(respuesta.getEstado()).isEqualTo(PropuestaTerminosPedido.PENDIENTE);
        verify(notificacionService).notificar(org.mockito.ArgumentMatchers.eq(creador), anyString(), anyString());
    }

    @Test
    @DisplayName("proponerTerminos rechaza si el contrato ya tiene alguna firma")
    void proponerTerminos_rechazaConContratoFirmado() {
        Contrato contrato = Contrato.builder().idContrato(5L).pedido(pedido).hashFirmaCliente("hash").build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(10L)).willReturn(Optional.of(contrato));
        PeticionCrearPropuestaTerminos peticion =
                PeticionCrearPropuestaTerminos.builder().precioPropuesto(new BigDecimal("35.00")).build();

        assertThatThrownBy(() -> pedidoServicio.proponerTerminos(10L, 1L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);

        verify(propuestaTerminosPedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("aceptarPropuestaTerminos aplica el cambio y genera el contrato si aun no existia")
    void aceptarPropuestaTerminos_aplicaCambioYGeneraContrato() {
        PropuestaTerminosPedido propuesta = PropuestaTerminosPedido.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(PropuestaTerminosPedido.PENDIENTE).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));
        given(contratoRepository.findByPedidoIdPedido(10L)).willReturn(Optional.empty());
        given(pedidoRepository.save(any(Pedido.class))).willAnswer(inv -> inv.getArgument(0));
        given(propuestaTerminosPedidoRepository.save(any(PropuestaTerminosPedido.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());
        given(contratoServicio.generarContrato(10L, 2L)).willReturn(RespuestaContrato.builder().idContrato(99L).build());

        // El creador (idUsuario=2) acepta la propuesta creada por el cliente (idUsuario=1).
        RespuestaPedido respuesta = pedidoServicio.aceptarPropuestaTerminos(10L, 7L, 2L);

        assertThat(respuesta.getPrecioPactado()).isEqualByComparingTo("35.00");
        assertThat(propuesta.getEstado()).isEqualTo(PropuestaTerminosPedido.ACEPTADA);
        verify(contratoServicio).generarContrato(10L, 2L);
        verify(notificacionService).notificar(org.mockito.ArgumentMatchers.eq(cliente), anyString(), anyString());
    }

    @Test
    @DisplayName("aceptarPropuestaTerminos no regenera el contrato si ya existia")
    void aceptarPropuestaTerminos_noRegeneraContratoExistente() {
        PropuestaTerminosPedido propuesta = PropuestaTerminosPedido.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(PropuestaTerminosPedido.PENDIENTE).build();
        Contrato contratoExistente = Contrato.builder().idContrato(3L).pedido(pedido).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));
        given(contratoRepository.findByPedidoIdPedido(10L)).willReturn(Optional.of(contratoExistente));
        given(pedidoRepository.save(any(Pedido.class))).willAnswer(inv -> inv.getArgument(0));
        given(propuestaTerminosPedidoRepository.save(any(PropuestaTerminosPedido.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        pedidoServicio.aceptarPropuestaTerminos(10L, 7L, 2L);

        verify(contratoServicio, never()).generarContrato(any(), any());
    }

    @Test
    @DisplayName("aceptarPropuestaTerminos rechaza la auto-aceptacion")
    void aceptarPropuestaTerminos_rechazaAutoAceptacion() {
        PropuestaTerminosPedido propuesta = PropuestaTerminosPedido.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(PropuestaTerminosPedido.PENDIENTE).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));

        assertThatThrownBy(() -> pedidoServicio.aceptarPropuestaTerminos(10L, 7L, 1L))
                .isInstanceOf(ExcepcionReglaNegocio.class);

        verify(pedidoRepository, never()).save(any());
        verifyNoInteractions(contratoServicio);
    }

    @Test
    @DisplayName("rechazarPropuestaTerminos marca la propuesta rechazada sin tocar el pedido")
    void rechazarPropuestaTerminos_marcaRechazadaSinTocarPedido() {
        PropuestaTerminosPedido propuesta = PropuestaTerminosPedido.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(PropuestaTerminosPedido.PENDIENTE).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));
        given(propuestaTerminosPedidoRepository.save(any(PropuestaTerminosPedido.class)))
                .willAnswer(inv -> inv.getArgument(0));

        RespuestaPropuestaTerminos respuesta = pedidoServicio.rechazarPropuestaTerminos(10L, 7L, 2L);

        assertThat(respuesta.getEstado()).isEqualTo(PropuestaTerminosPedido.RECHAZADA);
        verify(pedidoRepository, never()).save(any());
        verify(notificacionService).notificar(org.mockito.ArgumentMatchers.eq(cliente), anyString(), anyString());
    }

    // ---------- obtenerPedidoPorId (IDOR, OBS-08) ----------

    @Test
    @DisplayName("obtenerPedidoPorId permite al cliente dueño consultar su pedido")
    void obtenerPedidoPorId_clientePuedeVer() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        assertThat(pedidoServicio.obtenerPedidoPorId(10L, 1L)).isNotNull();
    }

    @Test
    @DisplayName("obtenerPedidoPorId permite al creador del servicio consultar el pedido")
    void obtenerPedidoPorId_creadorPuedeVer() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        assertThat(pedidoServicio.obtenerPedidoPorId(10L, 2L)).isNotNull();
    }

    @Test
    @DisplayName("obtenerPedidoPorId permite a un ADMIN autenticado consultar pedidos ajenos")
    void obtenerPedidoPorId_adminPuedeVer() {
        autenticarComo("admin@test.com", "ROLE_ADMIN");
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        assertThat(pedidoServicio.obtenerPedidoPorId(10L, 999L)).isNotNull();
    }

    @Test
    @DisplayName("1.2: obtenerPedidoPorId deriva etapaActual del último elemento del historial, sin consulta aparte")
    void obtenerPedidoPorId_etapaActualDelHistorial() {
        HistorialEstadoPedido h1 = HistorialEstadoPedido.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        EtapaFlujo etapaRevision = EtapaFlujo.builder().idEtapa(2L).nombreEtapa("Revisión").build();
        HistorialEstadoPedido h2 = HistorialEstadoPedido.builder().idHistorialEstado(2L).pedido(pedido).etapa(etapaRevision).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of(h1, h2));

        RespuestaPedido respuesta = pedidoServicio.obtenerPedidoPorId(10L, 1L);

        assertThat(respuesta.getEtapaActual()).isEqualTo("Revisión");
        verify(historialRepository, never()).findTopByPedidoIdPedidoOrderByFechaTransicionDesc(any());
    }

    @Test
    @DisplayName("1.2: obtenerPedidoPorId devuelve \"Sin estado\" cuando el pedido no tiene historial")
    void obtenerPedidoPorId_sinHistorial_etapaActualSinEstado() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        RespuestaPedido respuesta = pedidoServicio.obtenerPedidoPorId(10L, 1L);

        assertThat(respuesta.getEtapaActual()).isEqualTo("Sin estado");
    }

    @Test
    @DisplayName("obtenerPedidoPorId rechaza a un usuario ajeno sin rol admin")
    void obtenerPedidoPorId_rechazaAjeno() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoServicio.obtenerPedidoPorId(10L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("obtenerPedidoPorId lanza recurso no encontrado si el pedido no existe")
    void obtenerPedidoPorId_inexistente() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.obtenerPedidoPorId(10L, 1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    // ---------- listarMisPedidos / listarMisComisiones ----------

    @Test
    @DisplayName("listarMisPedidos mapea los pedidos del cliente")
    void listarMisPedidos_mapea() {
        given(pedidoRepository.findByUsuarioClienteIdUsuario(1L)).willReturn(List.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.empty());

        List<RespuestaPedidoResumido> resultado = pedidoServicio.listarMisPedidos(1L);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("listarMisComisiones mapea los pedidos del creador")
    void listarMisComisiones_mapea() {
        given(pedidoRepository.findByServicioPerfilUsuarioIdUsuario(2L)).willReturn(List.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.empty());

        assertThat(pedidoServicio.listarMisComisiones(2L)).hasSize(1);
    }

    // ---------- exportarMisComisiones (1.4) ----------

    @Test
    @DisplayName("1.4: exportarMisComisiones sin idsPedido exporta todas las comisiones del creador")
    void exportarMisComisiones_sinIds_exportaTodas() {
        Pedido pedidoOtro = Pedido.builder().idPedido(11L).usuarioCliente(cliente).servicio(servicio)
                .flujo(flujo).precioPactado(new BigDecimal("30.00")).build();
        given(pedidoRepository.findByServicioPerfilUsuarioIdUsuario(2L)).willReturn(List.of(pedido, pedidoOtro));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(any())).willReturn(Optional.empty());
        given(servicioExportacion.exportar(any(), any()))
                .willReturn(new DocumentoGenerado(new byte[0], "text/csv", "comisiones.csv"));

        pedidoServicio.exportarMisComisiones(2L, null, FormatoReporte.CSV, "creador@test.dev");

        org.mockito.ArgumentCaptor<ModeloReporte> captor = org.mockito.ArgumentCaptor.forClass(ModeloReporte.class);
        verify(servicioExportacion).exportar(captor.capture(), org.mockito.ArgumentMatchers.eq(FormatoReporte.CSV));
        assertThat(captor.getValue().getFilas()).hasSize(2);
    }

    @Test
    @DisplayName("1.4: exportarMisComisiones con idsPedido exporta exactamente lo filtrado en pantalla")
    void exportarMisComisiones_conIds_exportaSoloEsosPedidos() {
        Pedido pedidoOtro = Pedido.builder().idPedido(11L).usuarioCliente(cliente).servicio(servicio)
                .flujo(flujo).precioPactado(new BigDecimal("30.00")).build();
        given(pedidoRepository.findByServicioPerfilUsuarioIdUsuario(2L)).willReturn(List.of(pedido, pedidoOtro));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(any())).willReturn(Optional.empty());
        given(servicioExportacion.exportar(any(), any()))
                .willReturn(new DocumentoGenerado(new byte[0], "text/csv", "comisiones.csv"));

        pedidoServicio.exportarMisComisiones(2L, List.of(10L), FormatoReporte.CSV, "creador@test.dev");

        org.mockito.ArgumentCaptor<ModeloReporte> captor = org.mockito.ArgumentCaptor.forClass(ModeloReporte.class);
        verify(servicioExportacion).exportar(captor.capture(), org.mockito.ArgumentMatchers.eq(FormatoReporte.CSV));
        List<RespuestaPedidoResumido> filas = captor.getValue().getFilas();
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getIdPedido()).isEqualTo(10L);
    }

    @Test
    @DisplayName("1.4: un id ajeno en idsPedido no filtra dentro de las comisiones del creador (no es IDOR)")
    void exportarMisComisiones_idAjeno_noAparece() {
        given(pedidoRepository.findByServicioPerfilUsuarioIdUsuario(2L)).willReturn(List.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.empty());
        given(servicioExportacion.exportar(any(), any()))
                .willReturn(new DocumentoGenerado(new byte[0], "text/csv", "comisiones.csv"));

        pedidoServicio.exportarMisComisiones(2L, List.of(999L), FormatoReporte.CSV, "creador@test.dev");

        org.mockito.ArgumentCaptor<ModeloReporte> captor = org.mockito.ArgumentCaptor.forClass(ModeloReporte.class);
        verify(servicioExportacion).exportar(captor.capture(), org.mockito.ArgumentMatchers.eq(FormatoReporte.CSV));
        assertThat(captor.getValue().getFilas()).isEmpty();
    }

    // ---------- avanzarEtapa ----------

    @Test
    @DisplayName("avanzarEtapa registra la transicion a la siguiente etapa configurada")
    void avanzarEtapa_avanza() {
        PeticionAvanzarEtapa peticion = PeticionAvanzarEtapa.builder().observacion("Listo para revision").build();
        HistorialEstadoPedido ultimo = HistorialEstadoPedido.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        FlujoEtapaConfig configInicial = FlujoEtapaConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial).numeroOrden(1).build();
        FlujoEtapaConfig configSiguiente = FlujoEtapaConfig.builder().idFlujoEtapa(2L).flujo(flujo).etapa(etapaSiguiente).numeroOrden(2).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.of(ultimo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(configInicial, configSiguiente));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoAndNumeroOrdenGreaterThanOrderByNumeroOrdenAsc(1L, 1))
                .willReturn(List.of(configSiguiente));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        RespuestaPedido respuesta = pedidoServicio.avanzarEtapa(10L, 2L, peticion);

        assertThat(respuesta).isNotNull();
        org.mockito.Mockito.verify(historialRepository).save(any(HistorialEstadoPedido.class));
    }

    @Test
    @DisplayName("avanzarEtapa rechaza si la etapa actual exige entregable y el pedido no tiene ninguno subido")
    void avanzarEtapa_rechazaSinEntregableRequerido() {
        PeticionAvanzarEtapa peticion = PeticionAvanzarEtapa.builder().build();
        HistorialEstadoPedido ultimo = HistorialEstadoPedido.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        FlujoEtapaConfig configInicial = FlujoEtapaConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial)
                .numeroOrden(1).requiereEntregable(true).build();
        FlujoEtapaConfig configSiguiente = FlujoEtapaConfig.builder().idFlujoEtapa(2L).flujo(flujo).etapa(etapaSiguiente).numeroOrden(2).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.of(ultimo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(configInicial, configSiguiente));
        given(entregableFinalRepository.existsByPedidoIdPedido(10L)).willReturn(false);

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 2L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("entregable");
        verify(historialRepository, never()).save(any(HistorialEstadoPedido.class));
    }

    @Test
    @DisplayName("avanzarEtapa permite avanzar si la etapa exige entregable pero ya se subio uno")
    void avanzarEtapa_permiteConEntregableSubido() {
        PeticionAvanzarEtapa peticion = PeticionAvanzarEtapa.builder().build();
        HistorialEstadoPedido ultimo = HistorialEstadoPedido.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        FlujoEtapaConfig configInicial = FlujoEtapaConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial)
                .numeroOrden(1).requiereEntregable(true).build();
        FlujoEtapaConfig configSiguiente = FlujoEtapaConfig.builder().idFlujoEtapa(2L).flujo(flujo).etapa(etapaSiguiente).numeroOrden(2).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.of(ultimo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(configInicial, configSiguiente));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoAndNumeroOrdenGreaterThanOrderByNumeroOrdenAsc(1L, 1))
                .willReturn(List.of(configSiguiente));
        given(entregableFinalRepository.existsByPedidoIdPedido(10L)).willReturn(true);
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        RespuestaPedido respuesta = pedidoServicio.avanzarEtapa(10L, 2L, peticion);

        assertThat(respuesta).isNotNull();
        verify(historialRepository).save(any(HistorialEstadoPedido.class));
    }

    @Test
    @DisplayName("avanzarEtapa rechaza a un usuario que no es el creador del servicio")
    void avanzarEtapa_rechazaNoCreador() {
        PeticionAvanzarEtapa peticion = PeticionAvanzarEtapa.builder().build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 999L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("avanzarEtapa rechaza si el pedido no tiene estado inicial")
    void avanzarEtapa_sinEstadoInicial() {
        PeticionAvanzarEtapa peticion = PeticionAvanzarEtapa.builder().build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 2L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("avanzarEtapa rechaza si ya esta en la etapa final")
    void avanzarEtapa_rechazaEtapaFinal() {
        PeticionAvanzarEtapa peticion = PeticionAvanzarEtapa.builder().build();
        HistorialEstadoPedido ultimo = HistorialEstadoPedido.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaSiguiente).build();
        FlujoEtapaConfig configFinal = FlujoEtapaConfig.builder().idFlujoEtapa(2L).flujo(flujo).etapa(etapaSiguiente).numeroOrden(2).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.of(ultimo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(configFinal));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoAndNumeroOrdenGreaterThanOrderByNumeroOrdenAsc(1L, 2))
                .willReturn(List.of());

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 2L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("avanzarEtapa rechaza en vez de retroceder si la etapa actual ya no esta en la configuracion del flujo")
    void avanzarEtapa_etapaActualYaNoEnConfig_rechazaEnVezDeRetroceder() {
        // Reproduce el escenario del bug: alguien borró la FlujoEtapaConfig de
        // la etapa en la que el pedido está detenido. Antes, obtenerOrdenActual
        // caía a orden=0 y avanzarEtapa tomaba la primera etapa del flujo como
        // "siguiente" — el pedido retrocedía en silencio de la etapa 2 a la 1.
        PeticionAvanzarEtapa peticion = PeticionAvanzarEtapa.builder().build();
        HistorialEstadoPedido ultimo = HistorialEstadoPedido.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaSiguiente).build();
        // La config del flujo ya no incluye "etapaSiguiente" (fue eliminada).
        FlujoEtapaConfig soloEtapaInicial = FlujoEtapaConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial).numeroOrden(1).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.of(ultimo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(soloEtapaInicial));

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 2L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("ya no forma parte del flujo");
        org.mockito.Mockito.verify(historialRepository, never()).save(any(HistorialEstadoPedido.class));
    }

    @Test
    @DisplayName("avanzarEtapa lanza recurso no encontrado si el pedido no existe")
    void avanzarEtapa_pedidoInexistente() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 2L, PeticionAvanzarEtapa.builder().build()))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    // ---------- obtenerHistorial ----------

    @Test
    @DisplayName("obtenerHistorial devuelve el historial ordenado cuando el pedido existe")
    void obtenerHistorial_devuelveLista() {
        HistorialEstadoPedido h = HistorialEstadoPedido.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of(h));

        List<RespuestaHistorialEstado> resultado = pedidoServicio.obtenerHistorial(10L, 1L);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("obtenerHistorial lanza recurso no encontrado si el pedido no existe")
    void obtenerHistorial_pedidoInexistente() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.obtenerHistorial(10L, 1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerHistorial rechaza a un usuario ajeno sin rol admin")
    void obtenerHistorial_rechazaAjeno() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoServicio.obtenerHistorial(10L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("obtenerHistorial permite a un ADMIN autenticado consultar pedidos ajenos")
    void obtenerHistorial_adminPuedeVer() {
        autenticarComo("admin@test.com", "ROLE_ADMIN");
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        assertThat(pedidoServicio.obtenerHistorial(10L, 999L)).isNotNull();
    }

    // ---------- obtenerSeguimiento ----------

    @Test
    @DisplayName("obtenerSeguimiento calcula el porcentaje de avance con historial")
    void obtenerSeguimiento_calculaPorcentaje() {
        HistorialEstadoPedido h = HistorialEstadoPedido.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        FlujoEtapaConfig config1 = FlujoEtapaConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial).numeroOrden(1).build();
        FlujoEtapaConfig config2 = FlujoEtapaConfig.builder().idFlujoEtapa(2L).flujo(flujo).etapa(etapaSiguiente).numeroOrden(2).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(config1, config2));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of(h));

        RespuestaSeguimientoPedido resultado = pedidoServicio.obtenerSeguimiento(10L, 1L);

        assertThat(resultado.getEtapaActual()).isEqualTo("Inicio");
        assertThat(resultado.getTotalEtapas()).isEqualTo(2);
        assertThat(resultado.getPorcentajeProgreso()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("obtenerSeguimiento reporta sin estado cuando no hay historial")
    void obtenerSeguimiento_sinHistorial() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        RespuestaSeguimientoPedido resultado = pedidoServicio.obtenerSeguimiento(10L, 1L);

        assertThat(resultado.getEtapaActual()).isEqualTo("Sin estado");
        assertThat(resultado.getPorcentajeProgreso()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("obtenerSeguimiento lanza recurso no encontrado si el pedido no existe")
    void obtenerSeguimiento_pedidoInexistente() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.obtenerSeguimiento(10L, 1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerSeguimiento rechaza a un usuario ajeno sin rol admin")
    void obtenerSeguimiento_rechazaAjeno() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoServicio.obtenerSeguimiento(10L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("obtenerSeguimiento permite a un ADMIN autenticado consultar pedidos ajenos")
    void obtenerSeguimiento_adminPuedeVer() {
        autenticarComo("admin@test.com", "ROLE_ADMIN");
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        assertThat(pedidoServicio.obtenerSeguimiento(10L, 999L)).isNotNull();
    }

    private void autenticarComo(String correo, String... authorities) {
        List<SimpleGrantedAuthority> roles = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        var auth = new UsernamePasswordAuthenticationToken(correo, "N/A", roles);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
