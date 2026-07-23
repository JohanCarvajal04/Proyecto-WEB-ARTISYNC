package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSalaChat;
import uteq.edu.ec.artisync.entity.comunicacion.InfraccionMensaje;
import uteq.edu.ec.artisync.entity.legal.Mensaje;
import uteq.edu.ec.artisync.entity.legal.SalaChat;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.comunicacion.InfraccionRepository;
import uteq.edu.ec.artisync.repository.legal.MensajeRepository;
import uteq.edu.ec.artisync.repository.legal.SalaChatRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
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
    @Mock private InfraccionRepository  infraccionRepo;
    @Mock private MensajeFilterService  mensajeFilterService;
    @Mock private NotificacionService   notificacionService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private Usuario remitente;
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

        pedido = Pedido.builder()
                .idPedido(10L)
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

        RespuestaMensaje respuesta = chatService.enviarMensaje(10L, 1L, "Hola, ¿cómo va el proyecto?");

        assertThat(respuesta.getCuerpoMensaje()).isEqualTo("Hola, ¿cómo va el proyecto?");
        verify(messagingTemplate).convertAndSend(eq("/topic/sala.100"), any(RespuestaMensaje.class));
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
        when(mensajeFilterService.detectarPatron(mensajeConTelefono)).thenReturn("TELEFONO");
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(remitente));
        when(infraccionRepo.save(any(InfraccionMensaje.class))).thenReturn(InfraccionMensaje.builder().build());
        when(infraccionRepo.countByUsuarioIdUsuarioAndFechaInfraccionAfter(eq(1L), any())).thenReturn(1L);

        assertThatThrownBy(() -> chatService.enviarMensaje(10L, 1L, mensajeConTelefono))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("datos de contacto");

        verify(infraccionRepo).save(any(InfraccionMensaje.class));
        verify(mensajeRepo, never()).save(any());
    }

    @Test
    @DisplayName("RF-15: 3 infracciones en 30 días → cuenta suspendida")
    void enviarMensaje_tresInfracciones_suspendeCuenta() {
        String mensajeConEmail = "Escríbeme a test@ejemplo.com";

        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));
        when(mensajeFilterService.contieneContacto(mensajeConEmail)).thenReturn(true);
        when(mensajeFilterService.detectarPatron(mensajeConEmail)).thenReturn("EMAIL");
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(remitente));
        when(infraccionRepo.save(any(InfraccionMensaje.class))).thenReturn(InfraccionMensaje.builder().build());
        // Simular que ya tiene 3 infracciones (umbral alcanzado)
        when(infraccionRepo.countByUsuarioIdUsuarioAndFechaInfraccionAfter(eq(1L), any())).thenReturn(3L);
        when(usuarioRepo.save(any(Usuario.class))).thenReturn(remitente);

        assertThatThrownBy(() -> chatService.enviarMensaje(10L, 1L, mensajeConEmail))
                .isInstanceOf(ExcepcionReglaNegocio.class);

        // Cuenta desactivada y notificación enviada
        assertThat(remitente.getEstadoCuenta()).isFalse();
        verify(notificacionService).notificar(eq(remitente), eq("CUENTA_SUSPENDIDA"), anyString());
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
    @DisplayName("obtenerEstadoSala — retorna estado correcto")
    void obtenerEstadoSala_retornaEstado() {
        when(salaChatRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(sala));

        RespuestaSalaChat estado = chatService.obtenerEstadoSala(10L);

        assertThat(estado.getIdSala()).isEqualTo(100L);
        assertThat(estado.getSalaActiva()).isTrue();
    }

    @Test
    @DisplayName("obtenerEstadoSala — sala inexistente lanza ExcepcionRecursoNoEncontrado")
    void obtenerEstadoSala_sinSala_lanzaExcepcion() {
        when(salaChatRepo.findByPedidoIdPedido(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.obtenerEstadoSala(99L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }
}
