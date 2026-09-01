package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaMensajeChat;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSalaChat;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.Mensaje;
import uteq.edu.ec.artisync.entity.legal.SalaChat;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.MensajeRepository;
import uteq.edu.ec.artisync.repository.legal.SalaChatRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.comunicacion.InfraccionService;
import uteq.edu.ec.artisync.service.comunicacion.MensajeFilterService;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ChatServiceImpl.
 * Verifica RF-14 (sala, cierre) y RF-15 (filtrado de contactos).
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock private SalaChatRepository    salaChatRepo;
    @Mock private MensajeRepository     mensajeRepo;
    @Mock private UsuarioRepository     usuarioRepo;
    @Mock private InfraccionService     infraccionService;
    @Mock private MensajeFilterService  mensajeFilterService;
    @Mock private NotificacionService   notificacionService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private static final Long ID_CREADOR = 2L;
    private static final Long ID_AJENO = 999L;

    private Usuario remitente;
    private Usuario creador;
    private Pedido  pedido;
    private SalaChat sala;

    @BeforeEach
    void setUp() {
        remitente = Usuario.builder()
                .idUsuario(1L)
                .nombres("Juan")
                .apellidos("Pérez")
                .correo("juan@example.com")
                .estadoCuenta(true)
                .build();

        // remitente es el cliente del pedido; el creador es otro usuario, para
        // poder probar que ambas partes tienen acceso al chat y un tercero no.
        creador = Usuario.builder().idUsuario(ID_CREADOR).nombres("Ana").apellidos("Gómez").build();
        PerfilCreador perfil = PerfilCreador.builder().usuario(creador).build();
        Servicio servicio = Servicio.builder().perfil(perfil).tituloServicio("Ilustración").build();

        pedido = Pedido.builder()
                .idPedido(10L)
                .usuarioCliente(remitente)
                .servicio(servicio)
                .build();

        sala = SalaChat.builder()
                .idSala(100L)
                .pedido(pedido)
                .salaActiva(true)
                .fechaApertura(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // crearSala
    // =========================================================================

    @Test
    @DisplayName("crearSala — crea nueva sala cuando no existe")
    void crearSala_cuandoNoExiste_creaYRetorna() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.empty());
        when(salaChatRepo.save(any(SalaChat.class))).thenReturn(sala);

        SalaChat resultado = chatService.crearSala(pedido);

        assertThat(resultado.getIdSala()).isEqualTo(100L);
        assertThat(resultado.getSalaActiva()).isTrue();
        verify(salaChatRepo).save(any(SalaChat.class));
    }

    @Test
    @DisplayName("crearSala — retorna sala existente sin duplicar")
    void crearSala_cuandoYaExiste_retornaSalaExistente() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        SalaChat resultado = chatService.crearSala(pedido);

        assertThat(resultado.getIdSala()).isEqualTo(100L);
        verify(salaChatRepo, never()).save(any());
    }

    // =========================================================================
    // cerrarSala
    // =========================================================================

    @Test
    @DisplayName("cerrarSala — desactiva sala y notifica vía WebSocket")
    void cerrarSala_desactivaSalaYNotifica() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(salaChatRepo.save(any(SalaChat.class))).thenReturn(sala);

        chatService.cerrarSala(10L);

        assertThat(sala.getSalaActiva()).isFalse();
        verify(messagingTemplate).convertAndSend(eq("/topic/sala.100"), (Object) any());
    }

    @Test
    @DisplayName("cerrarSala — no lanza error si no existe sala")
    void cerrarSala_sinSala_noLanzaError() {
        when(salaChatRepo.findByPedidoIdPedido(99L)).thenReturn(Optional.empty());
        assertThatCode(() -> chatService.cerrarSala(99L)).doesNotThrowAnyException();
    }

    // =========================================================================
    // enviarMensaje — RF-14
    // =========================================================================

    @Test
    @DisplayName("enviarMensaje — mensaje limpio se persiste y publica en WebSocket")
    void enviarMensaje_sinContacto_persisteYPublica() {
        Mensaje msg = Mensaje.builder()
                .idMensaje(1L)
                .sala(sala)
                .remitente(remitente)
                .cuerpoMensaje("Hola, ¿cómo va el proyecto?")
                .leido(false)
                .fechaHoraEnvio(LocalDateTime.now())
                .build();

        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeFilterService.contieneContacto(anyString())).thenReturn(false);
        when(usuarioRepo.getReferenceById(1L)).thenReturn(remitente);
        when(mensajeRepo.save(any(Mensaje.class))).thenReturn(msg);

        RespuestaMensajeChat respuesta = chatService.enviarMensaje(10L, 1L, "Hola, ¿cómo va el proyecto?");

        assertThat(respuesta.getCuerpoMensaje()).isEqualTo("Hola, ¿cómo va el proyecto?");
        verify(messagingTemplate).convertAndSend(eq("/topic/sala.100"), any(RespuestaMensajeChat.class));
        // El remitente (1L) es el cliente: la notificación debe ir al creador (2L), no a él mismo.
        verify(notificacionService).notificar(eq(creador), eq("MENSAJE_RECIBIDO"), anyString());
        verify(notificacionService, never()).notificar(eq(remitente), eq("MENSAJE_RECIBIDO"), anyString());
    }

    @Test
    @DisplayName("enviarMensaje — cuando escribe el creador, notifica al cliente")
    void enviarMensaje_delCreador_notificaAlCliente() {
        Mensaje msg = Mensaje.builder()
                .idMensaje(2L)
                .sala(sala)
                .remitente(creador)
                .cuerpoMensaje("Ya tengo el boceto listo")
                .leido(false)
                .fechaHoraEnvio(LocalDateTime.now())
                .build();

        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeFilterService.contieneContacto(anyString())).thenReturn(false);
        when(usuarioRepo.getReferenceById(ID_CREADOR)).thenReturn(creador);
        when(mensajeRepo.save(any(Mensaje.class))).thenReturn(msg);

        chatService.enviarMensaje(10L, ID_CREADOR, "Ya tengo el boceto listo");

        verify(notificacionService).notificar(eq(remitente), eq("MENSAJE_RECIBIDO"), anyString());
    }

    @Test
    @DisplayName("enviarMensaje — el mensaje de la notificacion se trunca si es muy largo")
    void enviarMensaje_notificacionTruncaMensajesLargos() {
        String mensajeLargo = "a".repeat(200);
        Mensaje msg = Mensaje.builder()
                .idMensaje(3L)
                .sala(sala)
                .remitente(remitente)
                .cuerpoMensaje(mensajeLargo)
                .leido(false)
                .fechaHoraEnvio(LocalDateTime.now())
                .build();

        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeFilterService.contieneContacto(anyString())).thenReturn(false);
        when(usuarioRepo.getReferenceById(1L)).thenReturn(remitente);
        when(mensajeRepo.save(any(Mensaje.class))).thenReturn(msg);

        chatService.enviarMensaje(10L, 1L, mensajeLargo);

        org.mockito.ArgumentCaptor<String> textoCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(notificacionService).notificar(eq(creador), eq("MENSAJE_RECIBIDO"), textoCaptor.capture());
        assertThat(textoCaptor.getValue()).contains("…").doesNotContain(mensajeLargo);
    }

    // =========================================================================
    // enviarMensaje — RF-15 (filtrado de contactos)
    // =========================================================================

    @Test
    @DisplayName("RF-15: mensaje con teléfono es rechazado y registra infracción")
    void enviarMensaje_conTelefono_rechazaYRegistraInfraccion() {
        String mensajeConTelefono = "Llámame al +593 99 123 4567";

        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeFilterService.contieneContacto(mensajeConTelefono)).thenReturn(true);

        assertThatThrownBy(() -> chatService.enviarMensaje(10L, 1L, mensajeConTelefono))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("datos de contacto");

        // El registro de la infraccion se delega a InfraccionService, que corre
        // en su propia transaccion (REQUIRES_NEW) para que quede confirmada
        // aunque este metodo termine lanzando la excepcion de arriba.
        verify(infraccionService).registrarInfraccion(1L, 10L, mensajeConTelefono);
        verify(mensajeRepo, never()).save(any());
    }

    @Test
    @DisplayName("RF-15: si InfraccionService lanza, el mensaje igual se rechaza (no se guarda ni se notifica al destinatario)")
    void enviarMensaje_conContacto_noPropagaMensajeAunqueFalleRegistroInfraccion() {
        String mensajeConEmail = "Escríbeme a test@ejemplo.com";

        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeFilterService.contieneContacto(mensajeConEmail)).thenReturn(true);

        assertThatThrownBy(() -> chatService.enviarMensaje(10L, 1L, mensajeConEmail))
                .isInstanceOf(ExcepcionReglaNegocio.class);

        verify(mensajeRepo, never()).save(any());
        verify(notificacionService, never()).notificar(any(), eq("MENSAJE_RECIBIDO"), anyString());
    }

    @Test
    @DisplayName("enviarMensaje en sala cerrada lanza ExcepcionReglaNegocio")
    void enviarMensaje_salaCerrada_lanzaExcepcion() {
        sala.setSalaActiva(false);
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        assertThatThrownBy(() -> chatService.enviarMensaje(10L, 1L, "Hola"))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("cerrada");
    }

    @Test
    @DisplayName("obtenerEstadoSala — retorna estado correcto al cliente")
    void obtenerEstadoSala_retornaEstadoAlCliente() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        RespuestaSalaChat estado = chatService.obtenerEstadoSala(10L, 1L);

        assertThat(estado.getIdSala()).isEqualTo(100L);
        assertThat(estado.getSalaActiva()).isTrue();
    }

    @Test
    @DisplayName("obtenerEstadoSala — retorna estado correcto al creador")
    void obtenerEstadoSala_retornaEstadoAlCreador() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        RespuestaSalaChat estado = chatService.obtenerEstadoSala(10L, ID_CREADOR);

        assertThat(estado.getIdSala()).isEqualTo(100L);
    }

    @Test
    @DisplayName("obtenerEstadoSala — rechaza a un usuario ajeno al pedido")
    void obtenerEstadoSala_usuarioAjeno_rechaza() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        assertThatThrownBy(() -> chatService.obtenerEstadoSala(10L, ID_AJENO))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("obtenerEstadoSala — sala inexistente lanza ExcepcionRecursoNoEncontrado")
    void obtenerEstadoSala_sinSala_lanzaExcepcion() {
        when(salaChatRepo.findByPedidoIdPedido(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.obtenerEstadoSala(99L, 1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    // =========================================================================
    // obtenerMensajes
    // =========================================================================

    @Test
    @DisplayName("obtenerMensajes — el cliente puede leer el historial")
    void obtenerMensajes_permiteAlCliente() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeRepo.findBySalaIdSalaOrderByFechaHoraEnvioAsc(100L)).thenReturn(List.of());

        assertThatCode(() -> chatService.obtenerMensajes(10L, 1L, org.springframework.data.domain.PageRequest.of(0, 10)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("obtenerMensajes — rechaza a un usuario ajeno al pedido")
    void obtenerMensajes_usuarioAjeno_rechaza() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        assertThatThrownBy(() -> chatService.obtenerMensajes(
                10L, ID_AJENO, org.springframework.data.domain.PageRequest.of(0, 10)))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    // =========================================================================
    // enviarMensaje — control de acceso
    // =========================================================================

    @Test
    @DisplayName("enviarMensaje — rechaza a un usuario ajeno al pedido")
    void enviarMensaje_usuarioAjeno_rechaza() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        assertThatThrownBy(() -> chatService.enviarMensaje(10L, ID_AJENO, "Hola"))
                .isInstanceOf(ExcepcionReglaNegocio.class);

        verify(mensajeRepo, never()).save(any());
    }
}
