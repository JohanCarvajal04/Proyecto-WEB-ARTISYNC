package uteq.edu.ec.artisync.service.pedido.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.pedido.SketchResponse;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.pedido.Sketch;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.repository.pedido.SketchRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;
import uteq.edu.ec.artisync.service.pedido.ISketchService;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SketchServiceImplTest {

    private static final Long ID_PEDIDO = 7L;
    private static final Long ID_CREADOR = 1L;
    private static final Long ID_CLIENTE = 2L;
    private static final Long ID_TERCERO = 99L;

    @Mock private SketchRepository bocetoRepository;
    @Mock private OrderRepository pedidoRepository;
    @Mock private DocumentStorage almacenamiento;
    @Mock private NotificationService notificacionService;

    @InjectMocks private SketchServiceImpl servicio;

    private Order pedido;

    @BeforeEach
    void setUp() {
        User creador = new User();
        creador.setIdUsuario(ID_CREADOR);
        CreatorProfile perfil = new CreatorProfile();
        perfil.setUsuario(creador);
        Offering servicioCatalogo = new Offering();
        servicioCatalogo.setPerfil(perfil);

        User cliente = new User();
        cliente.setIdUsuario(ID_CLIENTE);

        pedido = new Order();
        pedido.setIdPedido(ID_PEDIDO);
        pedido.setServicio(servicioCatalogo);
        pedido.setUsuarioCliente(cliente);
    }

    private MockMultipartFile imagen(String nombre) {
        return new MockMultipartFile(nombre, nombre + ".png", "image/png", "contenido".getBytes());
    }

    private Sketch bocetoGuardado(String urlImagen) {
        return Sketch.builder().idBoceto(1L).pedido(pedido).urlImagen(urlImagen).build();
    }

    // ── Subida ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("subirBoceto crea un registro nuevo cuando el pedido no tiene boceto todavia")
    void subirBoceto_sinBocetoPrevio_creaUno() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(bocetoRepository.findByPedidoIdPedido(ID_PEDIDO)).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), eq("bocetos"))).thenReturn("bocetos/nuevo.png");
        when(bocetoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());

        SketchResponse respuesta = servicio.subirBoceto(ID_PEDIDO, ID_CREADOR, imagen("boceto"));

        assertThat(respuesta.getIdPedido()).isEqualTo(ID_PEDIDO);
        verify(almacenamiento, never()).eliminar(anyString());
    }

    @Test
    @DisplayName("subirBoceto reemplaza el boceto existente y borra la referencia anterior")
    void subirBoceto_sobreUnoExistente_borraLaReferenciaAnterior() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(bocetoRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(bocetoGuardado("bocetos/vieja.png")));
        when(almacenamiento.guardar(any(), eq("bocetos"))).thenReturn("bocetos/nueva.png");
        when(bocetoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());

        servicio.subirBoceto(ID_PEDIDO, ID_CREADOR, imagen("boceto"));

        verify(almacenamiento).eliminar("bocetos/vieja.png");
    }

    @Test
    @DisplayName("subirBoceto rechaza a un usuario que no es el creador del servicio")
    void subirBoceto_usuarioQueNoEsElCreador_esRechazado() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));

        assertThrows(BusinessRuleException.class,
                () -> servicio.subirBoceto(ID_PEDIDO, ID_TERCERO, imagen("boceto")));

        verify(almacenamiento, never()).guardar(any(), anyString());
    }

    @Test
    @DisplayName("subirBoceto rechaza un formato no permitido antes de tocar la base")
    void subirBoceto_formatoNoPermitido_seRechazaAntesDeTocarLaBase() {
        MockMultipartFile ejecutable = new MockMultipartFile(
                "imagen", "virus.exe", "application/x-msdownload", "MZ".getBytes());

        assertThrows(BusinessRuleException.class,
                () -> servicio.subirBoceto(ID_PEDIDO, ID_CREADOR, ejecutable));

        verifyNoInteractions(pedidoRepository, almacenamiento);
    }

    // ── Consulta ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerBoceto lanza recurso no encontrado si el pedido no tiene boceto")
    void obtenerBoceto_sinBoceto_lanzaRecursoNoEncontrado() {
        when(bocetoRepository.findByPedidoIdPedido(ID_PEDIDO)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> servicio.obtenerBoceto(ID_PEDIDO, ID_CLIENTE));
    }

    @Test
    @DisplayName("obtenerBoceto rechaza a un tercero sin relacion con el pedido")
    void obtenerBoceto_terceroSinRelacionConElPedido_esRechazado() {
        when(bocetoRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(bocetoGuardado("bocetos/b.png")));

        assertThrows(BusinessRuleException.class,
                () -> servicio.obtenerBoceto(ID_PEDIDO, ID_TERCERO));
    }

    @Test
    @DisplayName("obtenerBoceto permite verlo tanto al cliente como al creador")
    void obtenerBoceto_clienteYCreadorPuedenVerlo() {
        when(bocetoRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(bocetoGuardado("bocetos/b.png")));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());

        assertThat(servicio.obtenerBoceto(ID_PEDIDO, ID_CLIENTE)).isNotNull();
        assertThat(servicio.obtenerBoceto(ID_PEDIDO, ID_CREADOR)).isNotNull();
    }

    // ── Descarga ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("descargarBoceto devuelve los bytes reales y su content-type")
    void descargarBoceto_devuelveBytesRealesYSuContentType() {
        when(bocetoRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(bocetoGuardado("bocetos/b.png")));
        when(almacenamiento.leer("bocetos/b.png")).thenReturn("png".getBytes());

        ISketchService.ArchivoDescargado archivo = servicio.descargarBoceto(ID_PEDIDO, ID_CLIENTE);

        assertThat(archivo.contenido()).isEqualTo("png".getBytes());
        assertThat(archivo.contentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("descargarBoceto rechaza a un tercero sin relacion con el pedido")
    void descargarBoceto_terceroSinRelacionConElPedido_esRechazado() {
        when(bocetoRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(bocetoGuardado("bocetos/b.png")));

        assertThrows(BusinessRuleException.class,
                () -> servicio.descargarBoceto(ID_PEDIDO, ID_TERCERO));

        verify(almacenamiento, never()).leer(anyString());
    }
}
