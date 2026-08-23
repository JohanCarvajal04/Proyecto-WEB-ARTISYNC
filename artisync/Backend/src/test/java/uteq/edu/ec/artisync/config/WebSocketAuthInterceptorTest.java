package uteq.edu.ec.artisync.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.SalaChat;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.legal.SalaChatRepository;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.security.CustomUserDetailsService;
import uteq.edu.ec.artisync.security.JwtService;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * El endpoint /ws es público (necesario para el handshake SockJS), así que
 * este interceptor es la única barrera real en dos puntos: CONNECT
 * (autenticación) y SUBSCRIBE a /topic/sala.* (autorización por pedido).
 * Fail-closed en ambos: cualquier duda rechaza, nunca deja pasar como
 * anónimo o como un tercero ajeno al pedido (ver comentario de la clase).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketAuthInterceptorTest {

    private static final Long ID_CLIENTE = 100L;
    private static final Long ID_CREADOR = 200L;
    private static final Long ID_AJENO = 999L;
    private static final Long ID_SALA = 7L;

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private SalaChatRepository salaChatRepository;
    @Mock private Claims claims;

    private WebSocketAuthInterceptor interceptor;
    private SalaChat sala;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtService, userDetailsService, salaChatRepository);

        Usuario cliente = Usuario.builder().idUsuario(ID_CLIENTE).build();
        Usuario creador = Usuario.builder().idUsuario(ID_CREADOR).build();
        PerfilCreador perfil = PerfilCreador.builder().usuario(creador).build();
        Servicio servicio = Servicio.builder().perfil(perfil).build();
        Pedido pedido = Pedido.builder().idPedido(1L).usuarioCliente(cliente).servicio(servicio).build();
        sala = SalaChat.builder().idSala(ID_SALA).pedido(pedido).salaActiva(true).build();
    }

    // ---------- CONNECT ----------

    @Test
    @DisplayName("rechaza el CONNECT sin header Authorization")
    void rechazaSinAuthorization() {
        Message<byte[]> connect = connectConHeader(null);

        assertThatThrownBy(() -> interceptor.preSend(connect, null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("rechaza el CONNECT con un token expirado")
    void rechazaTokenExpirado() {
        given(jwtService.extraerTodosLosClaims("expirado")).willReturn(claims);
        given(jwtService.extraerUsername("expirado")).willReturn("user@test.com");
        given(claims.getExpiration()).willReturn(new Date(System.currentTimeMillis() - 60_000));

        Message<byte[]> connect = connectConHeader("Bearer expirado");

        assertThatThrownBy(() -> interceptor.preSend(connect, null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("rechaza el CONNECT cuando el JWT no se puede parsear")
    void rechazaTokenInvalido() {
        given(jwtService.extraerTodosLosClaims("basura")).willThrow(new RuntimeException("malformado"));

        Message<byte[]> connect = connectConHeader("Bearer basura");

        assertThatThrownBy(() -> interceptor.preSend(connect, null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("acepta el CONNECT y fija el Principal con un JWT valido")
    void aceptaTokenValido() {
        UserDetails userDetails = new CustomUserDetails(
                ID_CLIENTE, "user@test.com", "n/a", true, true, true, true, Collections.emptyList());
        given(jwtService.extraerTodosLosClaims("valido")).willReturn(claims);
        given(jwtService.extraerUsername("valido")).willReturn("user@test.com");
        given(claims.getExpiration()).willReturn(new Date(System.currentTimeMillis() + 60_000));
        given(userDetailsService.loadUserByUsername("user@test.com")).willReturn(userDetails);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valido");
        // El pipeline real de Spring construye el CONNECT con el accessor aún
        // mutable (setLeaveMutable): así es como preSend puede fijar el
        // usuario en accessor.setUser(). Con MessageBuilder normal los headers
        // ya vienen congelados y setUser() explota con "Already immutable".
        accessor.setLeaveMutable(true);
        Message<byte[]> connect = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(connect, null);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("no interviene en comandos sin destino de sala (p.ej. SEND)")
    void ignoraOtrosComandos() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<byte[]> send = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> resultado = interceptor.preSend(send, null);

        assertThat(resultado).isSameAs(send);
    }

    // ---------- SUBSCRIBE ----------

    @Test
    @DisplayName("permite suscribirse a la sala al cliente del pedido")
    void permiteSuscripcionAlCliente() {
        given(salaChatRepository.findById(ID_SALA)).willReturn(Optional.of(sala));
        Message<byte[]> subscribe = subscribeConUsuario("/topic/sala." + ID_SALA, ID_CLIENTE);

        Message<?> resultado = interceptor.preSend(subscribe, null);

        assertThat(resultado).isSameAs(subscribe);
    }

    @Test
    @DisplayName("permite suscribirse a la sala al creador del pedido")
    void permiteSuscripcionAlCreador() {
        given(salaChatRepository.findById(ID_SALA)).willReturn(Optional.of(sala));
        Message<byte[]> subscribe = subscribeConUsuario("/topic/sala." + ID_SALA, ID_CREADOR);

        Message<?> resultado = interceptor.preSend(subscribe, null);

        assertThat(resultado).isSameAs(subscribe);
    }

    @Test
    @DisplayName("rechaza la suscripcion de un usuario ajeno al pedido")
    void rechazaSuscripcionDeUsuarioAjeno() {
        given(salaChatRepository.findById(ID_SALA)).willReturn(Optional.of(sala));
        Message<byte[]> subscribe = subscribeConUsuario("/topic/sala." + ID_SALA, ID_AJENO);

        assertThatThrownBy(() -> interceptor.preSend(subscribe, null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("rechaza la suscripcion a una sala que no existe")
    void rechazaSuscripcionSalaInexistente() {
        given(salaChatRepository.findById(404L)).willReturn(Optional.empty());
        Message<byte[]> subscribe = subscribeConUsuario("/topic/sala.404", ID_CLIENTE);

        assertThatThrownBy(() -> interceptor.preSend(subscribe, null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("rechaza la suscripcion sin sesion autenticada")
    void rechazaSuscripcionSinPrincipal() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/sala." + ID_SALA);
        Message<byte[]> subscribe = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(subscribe, null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("no interviene en suscripciones fuera de /topic/sala.*")
    void ignoraSuscripcionesAOtrosDestinos() {
        Message<byte[]> subscribe = subscribeConUsuario("/user/queue/notificaciones", ID_CLIENTE);

        Message<?> resultado = interceptor.preSend(subscribe, null);

        assertThat(resultado).isSameAs(subscribe);
    }

    private Message<byte[]> connectConHeader(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    /** Simula un SUBSCRIBE en una sesión ya autenticada (el Principal que dejó el CONNECT). */
    private Message<byte[]> subscribeConUsuario(String destino, Long idUsuario) {
        CustomUserDetails userDetails = new CustomUserDetails(
                idUsuario, "user" + idUsuario + "@test.com", "n/a", true, true, true, true, Collections.emptyList());
        UsernamePasswordAuthenticationToken principal =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destino);
        accessor.setUser(principal);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
