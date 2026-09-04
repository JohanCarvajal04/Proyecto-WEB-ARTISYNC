package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEntregable;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.EntregableFinal;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.legal.EntregableFinalRepository;
import uteq.edu.ec.artisync.repository.legal.PagoGarantiaRepository;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.service.legal.IEntregableServicio;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntregableServicioImplTest {

    private static final Long ID_PEDIDO = 7L;
    private static final Long ID_CREADOR = 1L;
    private static final Long ID_CLIENTE = 2L;
    private static final Long ID_TERCERO = 99L;

    @Mock private EntregableFinalRepository entregableRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private PagoGarantiaRepository pagoGarantiaRepository;
    @Mock private ContratoRepository contratoRepository;
    @Mock private TransaccionPagoRepository transaccionPagoRepository;
    @Mock private AlmacenamientoDocumentos almacenamiento;
    @Mock private uteq.edu.ec.artisync.service.comunicacion.ChatService chatService;
    @Mock private uteq.edu.ec.artisync.service.comunicacion.NotificacionService notificacionService;

    @InjectMocks private EntregableServicioImpl servicio;

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        Usuario creador = new Usuario();
        creador.setIdUsuario(ID_CREADOR);
        PerfilCreador perfil = new PerfilCreador();
        perfil.setUsuario(creador);
        Servicio servicioCatalogo = new Servicio();
        servicioCatalogo.setPerfil(perfil);

        Usuario cliente = new Usuario();
        cliente.setIdUsuario(ID_CLIENTE);

        pedido = new Pedido();
        pedido.setIdPedido(ID_PEDIDO);
        pedido.setServicio(servicioCatalogo);
        pedido.setUsuarioCliente(cliente);
    }

    private MockMultipartFile imagen(String nombre) {
        return new MockMultipartFile(nombre, nombre + ".png", "image/png", "contenido".getBytes());
    }

    private EntregableFinal entregableGuardado(String marcaAgua, String limpia, boolean liberado) {
        return EntregableFinal.builder()
                .idEntregable(1L)
                .pedido(pedido)
                .urlVersionMarcaAgua(marcaAgua)
                .urlVersionLimpia(limpia)
                .estaLiberado(liberado)
                .build();
    }

    // ── Subida ───────────────────────────────────────────────────────────────

    @Test
    void subirEntregable_guardaAmbasVersionesBajoElPrefijoDeEntregables() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO)).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), eq("entregables")))
                .thenReturn("entregables/marca.png", "entregables/limpia.png");
        when(entregableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());

        RespuestaEntregable respuesta = servicio.subirEntregable(
                ID_PEDIDO, ID_CREADOR, imagen("marca"), imagen("limpia"));

        verify(almacenamiento, times(2)).guardar(any(), eq("entregables"));
        assertThat(respuesta.getIdPedido()).isEqualTo(ID_PEDIDO);
    }

    @Test
    void subirEntregable_usuarioQueNoEsElCreador_esRechazado() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));

        assertThrows(ExcepcionReglaNegocio.class, () -> servicio.subirEntregable(
                ID_PEDIDO, ID_TERCERO, imagen("marca"), imagen("limpia")));

        verify(almacenamiento, never()).guardar(any(), anyString());
    }

    @Test
    void subirEntregable_formatoNoPermitido_seRechazaAntesDeTocarLaBase() {
        MockMultipartFile ejecutable = new MockMultipartFile(
                "versionLimpia", "virus.exe", "application/x-msdownload", "MZ".getBytes());

        assertThrows(ExcepcionReglaNegocio.class, () -> servicio.subirEntregable(
                ID_PEDIDO, ID_CREADOR, imagen("marca"), ejecutable));

        verifyNoInteractions(pedidoRepository, almacenamiento);
    }

    /** Resubir no debe dejar los archivos anteriores facturándose en Azure. */
    @Test
    void subirEntregable_sobreUnoExistente_borraLasReferenciasAnteriores() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/vieja-marca.png",
                        "entregables/vieja-limpia.png", false)));
        when(almacenamiento.guardar(any(), eq("entregables")))
                .thenReturn("entregables/nueva-marca.png", "entregables/nueva-limpia.png");
        when(entregableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());

        servicio.subirEntregable(ID_PEDIDO, ID_CREADOR, imagen("marca"), imagen("limpia"));

        verify(almacenamiento).eliminar("entregables/vieja-marca.png");
        verify(almacenamiento).eliminar("entregables/vieja-limpia.png");
    }

    @Test
    void subirEntregable_siFallaBorrarLoViejo_laSubidaIgualSeCompleta() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/vieja.png", "entregables/vieja2.png", false)));
        when(almacenamiento.guardar(any(), eq("entregables")))
                .thenReturn("entregables/nueva.png", "entregables/nueva2.png");
        when(entregableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());
        doThrow(new ExcepcionReglaNegocio("Azure caido")).when(almacenamiento).eliminar(anyString());

        RespuestaEntregable respuesta = servicio.subirEntregable(
                ID_PEDIDO, ID_CREADOR, imagen("marca"), imagen("limpia"));

        assertThat(respuesta).isNotNull();
    }

    // ── Descarga de la version limpia ────────────────────────────────────────

    @Test
    void descargarVersionLimpia_sinPagoLiberado_esRechazada() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", false)));

        assertThrows(ExcepcionReglaNegocio.class,
                () -> servicio.descargarVersionLimpia(ID_PEDIDO, ID_CLIENTE));

        verify(almacenamiento, never()).leer(anyString());
    }

    @Test
    void descargarVersionLimpia_liberada_devuelveBytesRealesYSuContentType() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.pdf", true)));
        when(almacenamiento.leer("entregables/l.pdf")).thenReturn("%PDF".getBytes());

        IEntregableServicio.ArchivoDescargado archivo =
                servicio.descargarVersionLimpia(ID_PEDIDO, ID_CLIENTE);

        assertThat(archivo.contenido()).isEqualTo("%PDF".getBytes());
        assertThat(archivo.contentType()).isEqualTo("application/pdf");
        assertThat(archivo.nombreSugerido()).isEqualTo("entregable-pedido-7.pdf");
    }

    @Test
    void descargarVersionLimpia_usuarioQueNoEsElCliente_esRechazado() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));

        assertThrows(ExcepcionReglaNegocio.class,
                () -> servicio.descargarVersionLimpia(ID_PEDIDO, ID_TERCERO));
    }

    // ── Descarga de la version con marca de agua ─────────────────────────────

    @Test
    void descargarMarcaAgua_clienteYCreadorPuedenVerlaSinPagoLiberado() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", false)));
        when(almacenamiento.leer("entregables/m.png")).thenReturn("png".getBytes());

        assertThat(servicio.descargarVersionMarcaAgua(ID_PEDIDO, ID_CLIENTE).contenido()).isNotEmpty();
        assertThat(servicio.descargarVersionMarcaAgua(ID_PEDIDO, ID_CREADOR).contenido()).isNotEmpty();
    }

    @Test
    void descargarMarcaAgua_terceroSinRelacionConElPedido_esRechazado() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", false)));

        assertThrows(ExcepcionReglaNegocio.class,
                () -> servicio.descargarVersionMarcaAgua(ID_PEDIDO, ID_TERCERO));

        verify(almacenamiento, never()).leer(anyString());
    }

    @Test
    void descargarMarcaAgua_entregableSinArchivo_reportaRecursoNoEncontrado() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado(null, "entregables/l.png", false)));

        assertThrows(ExcepcionRecursoNoEncontrado.class,
                () -> servicio.descargarVersionMarcaAgua(ID_PEDIDO, ID_CLIENTE));
    }

    // ── Respuesta ────────────────────────────────────────────────────────────

    /** Con Azure el archivo viaja directo desde el blob, sin pasar por el backend. */
    @Test
    void obtenerEntregable_conProveedorQueFirmaUrls_devuelveElSasYNoLaRutaDelBackend() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", true)));
        when(almacenamiento.urlTemporal("entregables/m.png"))
                .thenReturn(Optional.of("https://cuenta.blob.core.windows.net/c/entregables/m.png?sig=x"));
        when(almacenamiento.urlTemporal("entregables/l.png"))
                .thenReturn(Optional.of("https://cuenta.blob.core.windows.net/c/entregables/l.png?sig=y"));

        RespuestaEntregable respuesta = servicio.obtenerEntregable(ID_PEDIDO, ID_CLIENTE);

        assertThat(respuesta.getUrlVersionMarcaAgua()).startsWith("https://").contains("sig=");
        assertThat(respuesta.getUrlVersionLimpia()).startsWith("https://").contains("sig=");
    }

    @Test
    void obtenerEntregable_conAlmacenamientoLocal_caeALaRutaDelBackend() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", true)));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());

        RespuestaEntregable respuesta = servicio.obtenerEntregable(ID_PEDIDO, ID_CLIENTE);

        assertThat(respuesta.getUrlVersionMarcaAgua())
                .isEqualTo("/api/v1/pedidos/7/entregable/descargar/marca-agua");
        assertThat(respuesta.getUrlVersionLimpia())
                .isEqualTo("/api/v1/pedidos/7/entregable/descargar");
    }

    /** El cliente no debe recibir la version limpia mientras no libere el pago. */
    @Test
    void obtenerEntregable_clienteSinLiberar_noRecibeLaVersionLimpia() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", false)));
        when(almacenamiento.urlTemporal("entregables/m.png")).thenReturn(Optional.empty());

        RespuestaEntregable respuesta = servicio.obtenerEntregable(ID_PEDIDO, ID_CLIENTE);

        assertThat(respuesta.getUrlVersionLimpia()).isNull();
        assertThat(respuesta.getUrlVersionMarcaAgua()).isNotNull();
    }

    // ── Aprobar Entrega ──────────────────────────────────────────────────────
    @Test
    void aprobarEntrega_ok() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedidoParaActualizar(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("m", "l", false)));

        uteq.edu.ec.artisync.entity.legal.Contrato contrato = new uteq.edu.ec.artisync.entity.legal.Contrato();
        contrato.setIdContrato(1L);
        when(contratoRepository.findByPedidoIdPedido(ID_PEDIDO)).thenReturn(Optional.of(contrato));

        uteq.edu.ec.artisync.entity.legal.PagoGarantia pago = new uteq.edu.ec.artisync.entity.legal.PagoGarantia();
        pago.setMontoRetenido(new java.math.BigDecimal("100.00"));
        when(pagoGarantiaRepository.findByContratoIdContrato(1L)).thenReturn(Optional.of(pago));

        servicio.aprobarEntrega(ID_PEDIDO, ID_CLIENTE);

        verify(transaccionPagoRepository, times(2)).save(any());
        verify(entregableRepository).save(any());
    }

    @Test
    void aprobarEntrega_noEsCliente_error() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));

        assertThrows(ExcepcionReglaNegocio.class, () -> servicio.aprobarEntrega(ID_PEDIDO, ID_TERCERO));
    }

    @Test
    void aprobarEntrega_yaLiberado_error() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedidoParaActualizar(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("m", "l", true)));

        assertThrows(ExcepcionReglaNegocio.class, () -> servicio.aprobarEntrega(ID_PEDIDO, ID_CLIENTE));
    }
}
