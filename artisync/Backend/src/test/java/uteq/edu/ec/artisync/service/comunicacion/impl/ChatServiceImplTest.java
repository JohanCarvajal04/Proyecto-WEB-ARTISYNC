package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.ChatMessageResponse;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.ChatRoomResponse;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.legal.Message;
import uteq.edu.ec.artisync.entity.legal.ChatRoom;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.MessageRepository;
import uteq.edu.ec.artisync.repository.legal.ChatRoomRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.comunicacion.ViolationService;
import uteq.edu.ec.artisync.service.comunicacion.MessageFilterService;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;

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

    @Mock private ChatRoomRepository    salaChatRepo;
    @Mock private MessageRepository     mensajeRepo;
    @Mock private UserRepository     usuarioRepo;
    @Mock private ViolationService     infraccionService;
    @Mock private MessageFilterService  mensajeFilterService;
    @Mock private NotificationService   notificacionService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private static final Long ID_CREADOR = 2L;
    private static final Long ID_AJENO = 999L;

    private User remitente;
    private User creador;
    private Order  pedido;
    private ChatRoom sala;

    @BeforeEach
    void setUp() {
        remitente = User.builder()
                .idUsuario(1L)
                .nombres("Juan")
                .apellidos("Pérez")
                .correo("juan@example.com")
                .estadoCuenta(true)
                .build();

        // remitente es el cliente del pedido; el creador es otro usuario, para
        // poder probar que ambas partes tienen acceso al chat y un tercero no.
        creador = User.builder().idUsuario(ID_CREADOR).nombres("Ana").apellidos("Gómez").build();
        CreatorProfile perfil = CreatorProfile.builder().usuario(creador).build();
        Offering servicio = Offering.builder().perfil(perfil).tituloServicio("Ilustración").build();

        pedido = Order.builder()
                .idPedido(10L)
                .usuarioCliente(remitente)
                .servicio(servicio)
                .build();

        sala = ChatRoom.builder()
                .idSala(100L)
                .pedido(pedido)
                .salaActiva(true)
                .fechaApertura(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // createRoom
    // =========================================================================

    @Test
    @DisplayName("createRoom — crea nueva sala cuando no existe")
    void crearSala_cuandoNoExiste_creaYRetorna() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.empty());
        when(salaChatRepo.save(any(ChatRoom.class))).thenReturn(sala);

        ChatRoom resultado = chatService.createRoom(pedido);

        assertThat(resultado.getIdSala()).isEqualTo(100L);
        assertThat(resultado.getSalaActiva()).isTrue();
        verify(salaChatRepo).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("createRoom — retorna sala existente sin duplicar")
    void crearSala_cuandoYaExiste_retornaSalaExistente() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        ChatRoom resultado = chatService.createRoom(pedido);

        assertThat(resultado.getIdSala()).isEqualTo(100L);
        verify(salaChatRepo, never()).save(any());
    }

    // =========================================================================
    // closeRoom
    // =========================================================================

    @Test
    @DisplayName("closeRoom — desactiva sala y notifica vía WebSocket")
    void cerrarSala_desactivaSalaYNotifica() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(salaChatRepo.save(any(ChatRoom.class))).thenReturn(sala);

        chatService.closeRoom(10L);

        assertThat(sala.getSalaActiva()).isFalse();
        verify(messagingTemplate).convertAndSend(eq("/topic/sala.100"), (Object) any());
    }

    @Test
    @DisplayName("closeRoom — no lanza error si no existe sala")
    void cerrarSala_sinSala_noLanzaError() {
        when(salaChatRepo.findByPedidoIdPedido(99L)).thenReturn(Optional.empty());
        assertThatCode(() -> chatService.closeRoom(99L)).doesNotThrowAnyException();
    }

    // =========================================================================
    // sendMessage — RF-14
    // =========================================================================

    @Test
    @DisplayName("sendMessage — mensaje limpio se persiste y publica en WebSocket")
    void enviarMensaje_sinContacto_persisteYPublica() {
        Message msg = Message.builder()
                .idMensaje(1L)
                .sala(sala)
                .remitente(remitente)
                .cuerpoMensaje("Hola, ¿cómo va el proyecto?")
                .leido(false)
                .fechaHoraEnvio(LocalDateTime.now())
                .build();

        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeFilterService.containsContactInfo(anyString())).thenReturn(false);
        when(usuarioRepo.getReferenceById(1L)).thenReturn(remitente);
        when(mensajeRepo.save(any(Message.class))).thenReturn(msg);

        ChatMessageResponse respuesta = chatService.sendMessage(10L, 1L, "Hola, ¿cómo va el proyecto?");

        assertThat(respuesta.getCuerpoMensaje()).isEqualTo("Hola, ¿cómo va el proyecto?");
        verify(messagingTemplate).convertAndSend(eq("/topic/sala.100"), any(ChatMessageResponse.class));
        // El remitente (1L) es el cliente: la notificación debe ir al creador (2L), no a él mismo.
        verify(notificacionService).notify(eq(creador), eq("MENSAJE_RECIBIDO"), anyString());
        verify(notificacionService, never()).notify(eq(remitente), eq("MENSAJE_RECIBIDO"), anyString());
    }

    @Test
    @DisplayName("sendMessage — cuando escribe el creador, notifica al cliente")
    void enviarMensaje_delCreador_notificaAlCliente() {
        Message msg = Message.builder()
                .idMensaje(2L)
                .sala(sala)
                .remitente(creador)
                .cuerpoMensaje("Ya tengo el boceto listo")
                .leido(false)
                .fechaHoraEnvio(LocalDateTime.now())
                .build();

        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeFilterService.containsContactInfo(anyString())).thenReturn(false);
        when(usuarioRepo.getReferenceById(ID_CREADOR)).thenReturn(creador);
        when(mensajeRepo.save(any(Message.class))).thenReturn(msg);

        chatService.sendMessage(10L, ID_CREADOR, "Ya tengo el boceto listo");

        verify(notificacionService).notify(eq(remitente), eq("MENSAJE_RECIBIDO"), anyString());
    }

    @Test
    @DisplayName("sendMessage — el mensaje de la notificacion se trunca si es muy largo")
    void enviarMensaje_notificacionTruncaMensajesLargos() {
        String mensajeLargo = "a".repeat(200);
        Message msg = Message.builder()
                .idMensaje(3L)
                .sala(sala)
                .remitente(remitente)
                .cuerpoMensaje(mensajeLargo)
                .leido(false)
                .fechaHoraEnvio(LocalDateTime.now())
                .build();

        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeFilterService.containsContactInfo(anyString())).thenReturn(false);
        when(usuarioRepo.getReferenceById(1L)).thenReturn(remitente);
        when(mensajeRepo.save(any(Message.class))).thenReturn(msg);

        chatService.sendMessage(10L, 1L, mensajeLargo);

        org.mockito.ArgumentCaptor<String> textoCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(notificacionService).notify(eq(creador), eq("MENSAJE_RECIBIDO"), textoCaptor.capture());
        assertThat(textoCaptor.getValue()).contains("…").doesNotContain(mensajeLargo);
    }

    // =========================================================================
    // sendMessage — RF-15 (filtrado de contactos)
    // =========================================================================

    @Test
    @DisplayName("RF-15: mensaje con teléfono es rechazado y registra infracción")
    void enviarMensaje_conTelefono_rechazaYRegistraInfraccion() {
        String mensajeConTelefono = "Llámame al +593 99 123 4567";

        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeFilterService.containsContactInfo(mensajeConTelefono)).thenReturn(true);

        assertThatThrownBy(() -> chatService.sendMessage(10L, 1L, mensajeConTelefono))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("datos de contacto");

        // El registro de la infraccion se delega a ViolationService, que corre
        // en su propia transaccion (REQUIRES_NEW) para que quede confirmada
        // aunque este metodo termine lanzando la excepcion de arriba.
        verify(infraccionService).registerViolation(1L, 10L, mensajeConTelefono);
        verify(mensajeRepo, never()).save(any());
    }

    @Test
    @DisplayName("RF-15: si ViolationService lanza, el mensaje igual se rechaza (no se guarda ni se notifica al destinatario)")
    void enviarMensaje_conContacto_noPropagaMensajeAunqueFalleRegistroInfraccion() {
        String mensajeConEmail = "Escríbeme a test@ejemplo.com";

        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeFilterService.containsContactInfo(mensajeConEmail)).thenReturn(true);

        assertThatThrownBy(() -> chatService.sendMessage(10L, 1L, mensajeConEmail))
                .isInstanceOf(BusinessRuleException.class);

        verify(mensajeRepo, never()).save(any());
        verify(notificacionService, never()).notify(any(), eq("MENSAJE_RECIBIDO"), anyString());
    }

    @Test
    @DisplayName("sendMessage en sala cerrada lanza BusinessRuleException")
    void enviarMensaje_salaCerrada_lanzaExcepcion() {
        sala.setSalaActiva(false);
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        assertThatThrownBy(() -> chatService.sendMessage(10L, 1L, "Hola"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cerrada");
    }

    @Test
    @DisplayName("getRoomStatus — retorna estado correcto al cliente")
    void obtenerEstadoSala_retornaEstadoAlCliente() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        ChatRoomResponse estado = chatService.getRoomStatus(10L, 1L);

        assertThat(estado.getIdSala()).isEqualTo(100L);
        assertThat(estado.getSalaActiva()).isTrue();
    }

    @Test
    @DisplayName("getRoomStatus — retorna estado correcto al creador")
    void obtenerEstadoSala_retornaEstadoAlCreador() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        ChatRoomResponse estado = chatService.getRoomStatus(10L, ID_CREADOR);

        assertThat(estado.getIdSala()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getRoomStatus — rechaza a un usuario ajeno al pedido")
    void obtenerEstadoSala_usuarioAjeno_rechaza() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        assertThatThrownBy(() -> chatService.getRoomStatus(10L, ID_AJENO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("getRoomStatus — sala inexistente lanza ResourceNotFoundException")
    void obtenerEstadoSala_sinSala_lanzaExcepcion() {
        when(salaChatRepo.findByPedidoIdPedido(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getRoomStatus(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // getMessages
    // =========================================================================

    @Test
    @DisplayName("getMessages — el cliente puede leer el historial")
    void obtenerMensajes_permiteAlCliente() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeRepo.findBySalaIdSalaOrderByFechaHoraEnvioAsc(100L)).thenReturn(List.of());

        assertThatCode(() -> chatService.getMessages(10L, 1L, org.springframework.data.domain.PageRequest.of(0, 10)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getMessages — rechaza a un usuario ajeno al pedido")
    void obtenerMensajes_usuarioAjeno_rechaza() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        assertThatThrownBy(() -> chatService.getMessages(
                10L, ID_AJENO, org.springframework.data.domain.PageRequest.of(0, 10)))
                .isInstanceOf(BusinessRuleException.class);
    }

    // =========================================================================
    // sendMessage — control de acceso
    // =========================================================================

    @Test
    @DisplayName("sendMessage — rechaza a un usuario ajeno al pedido")
    void enviarMensaje_usuarioAjeno_rechaza() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        assertThatThrownBy(() -> chatService.sendMessage(10L, ID_AJENO, "Hola"))
                .isInstanceOf(BusinessRuleException.class);

        verify(mensajeRepo, never()).save(any());
    }
}
