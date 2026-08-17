package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.controller.seguridad.*;
import uteq.edu.ec.artisync.service.seguridad.*;
import uteq.edu.ec.artisync.service.seguridad.impl.*;
import uteq.edu.ec.artisync.service.shared.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.seguridad.request.LoginRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.RegisterRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.TwoFactorRequest;
import uteq.edu.ec.artisync.dto.seguridad.response.TokenResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.dto.seguridad.request.ForgotPasswordRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.ResetPasswordRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.entity.seguridad.TokenRecuperacion;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.security.CustomUserDetailsService;
import uteq.edu.ec.artisync.security.JwtService;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private TokenRecuperacionRepository tokenRecuperacionRepository;
    @Mock
    private SesionUsuarioRepository sesionUsuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private SessionRevocationService sessionRevocationService;
    @Mock
    private IntentosAutenticacionService intentosAutenticacionService;
    @Mock
    private PreAuth2faTicketService preAuth2faTicketService;
    @Mock
    private TwoFactorService twoFactorService;
    @Mock
    private EmailService emailService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private Usuario usuario;
    private Rol rolCliente;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setNombres("Juan");
        registerRequest.setApellidos("Perez");
        registerRequest.setCorreo("juan@example.com");
        registerRequest.setContrasena("Password123!");
        registerRequest.setFechaNacimiento(LocalDate.of(2000, 1, 1));
        registerRequest.setRol("CLIENTE");

        usuario = Usuario.builder()
                .idUsuario(1L)
                .nombres("Juan")
                .apellidos("Perez")
                .correo("juan@example.com")
                .contrasenaHash("hashed")
                .estadoCuenta(true)
                .build();

        rolCliente = Rol.builder()
                .idRol(1L)
                .nombreRol("CLIENTE")
                .build();
    }

    /** Simula lo que Spring Data envuelve cuando fn_x lanza RAISE EXCEPTION ... USING ERRCODE = '...'. */
    private static RuntimeException excepcionSql(String sqlState, String mensaje) {
        return new RuntimeException(new SQLException(mensaje, sqlState));
    }

    /** JSON que produciría fn_resolver_estado_login para el usuario de prueba. */
    private static String estadoLoginJson(Long idUsuario, String correo, boolean dosFactoresHabilitado, String... roles) {
        String rolesJson = String.join(",", List.of(roles).stream().map(r -> "\"" + r + "\"").toList());
        return "{\"idUsuario\":" + idUsuario + ",\"correo\":\"" + correo + "\",\"nombres\":\"Juan\",\"apellidos\":\"Perez\","
                + "\"estadoCuenta\":true,\"dosFactoresHabilitado\":" + dosFactoresHabilitado + ",\"roles\":[" + rolesJson + "]}";
    }

    // ── register (REQ-F-001 / fn_registrar_usuario) ─────────────────────────

    @Test
    void register_ShouldThrowException_WhenUserIsMinorRNF12() {
        registerRequest.setFechaNacimiento(LocalDate.now().minusYears(15));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.registrarUsuario(any(), any(), any(), any(), any(), any()))
                .thenThrow(excepcionSql("23514", "Debes tener al menos 18 anios para registrarte en ARTISYNC (RNF-12)"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.register(registerRequest));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("18"));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.registrarUsuario(any(), any(), any(), any(), any(), any()))
                .thenThrow(excepcionSql("23505", "El correo juan@example.com ya esta registrado en la plataforma"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.register(registerRequest));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void register_ShouldRegisterUserSuccessfully() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.registrarUsuario(eq("Juan"), eq("Perez"), eq("juan@example.com"), eq("hashed"), any(), eq("CLIENTE")))
                .thenReturn(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UserResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals(1L, response.getIdUsuario());
        assertEquals("juan@example.com", response.getCorreo());
    }

    @Test
    void register_ShouldCreatePerfilCreador_WhenRolIsCreador() {
        registerRequest.setRol("CREADOR");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.registrarUsuario(any(), any(), any(), any(), any(), eq("CREADOR"))).thenReturn(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UserResponse response = authService.register(registerRequest);

        assertEquals(List.of("CREADOR"), response.getRoles());
        verify(usuarioRepository).registrarUsuario(any(), any(), any(), any(), any(), eq("CREADOR"));
    }

    @Test
    void register_ShouldDefaultToCliente_WhenRolIsBlank() {
        registerRequest.setRol("");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.registrarUsuario(any(), any(), any(), any(), any(), eq("CLIENTE"))).thenReturn(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UserResponse response = authService.register(registerRequest);

        assertEquals(List.of("CLIENTE"), response.getRoles());
    }

    @Test
    void register_ShouldRejectRolNoPermitido() {
        registerRequest.setRol("ADMIN");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.registrarUsuario(any(), any(), any(), any(), any(), eq("ADMIN")))
                .thenThrow(excepcionSql("23514", "Rol no permitido en registro. Solo se permiten CLIENTE o CREADOR: ADMIN"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(registerRequest));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void register_ShouldThrowBadRequest_WhenRolNoExisteEnBD() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.registrarUsuario(any(), any(), any(), any(), any(), any()))
                .thenThrow(excepcionSql("23503", "El rol especificado no existe en el sistema: CLIENTE"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(registerRequest));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    // ── login (REQ-F-002 / fn_resolver_estado_login) ────────────────────────

    @Test
    void login_ShouldLoginSuccessfully() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setCorreo("juan@example.com");
        loginRequest.setContrasena("Password123!");

        when(usuarioRepository.resolverEstadoLogin("juan@example.com"))
                .thenReturn(estadoLoginJson(1L, "juan@example.com", false, "CLIENTE"));
        when(usuarioRepository.getReferenceById(1L)).thenReturn(usuario);

        UserDetails userDetails = new User("juan@example.com", "hashed", List.of(new SimpleGrantedAuthority("CLIENTE")));
        when(userDetailsService.loadUserByUsername("juan@example.com")).thenReturn(userDetails);
        when(jwtService.generarToken(userDetails)).thenReturn("access-token");
        when(jwtService.generarRefreshToken(userDetails)).thenReturn("refresh-token");

        TokenResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertFalse(response.isRequiere2fa());
        assertEquals(List.of("CLIENTE"), response.getRoles());
        verify(intentosAutenticacionService).limpiar("login-cuenta", "juan@example.com");
        verify(intentosAutenticacionService, never()).verificarCuota(anyString(), anyString(), anyInt(), any());
    }

    @Test
    void login_ShouldRegisterFailedAttempt_WhenCredentialsInvalid() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setCorreo("juan@example.com");
        loginRequest.setContrasena("password-incorrecto");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Credenciales inválidas"));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.login(loginRequest));

        verify(intentosAutenticacionService).verificarCuota("login-cuenta", "juan@example.com", 5, java.time.Duration.ofMinutes(15));
        verify(intentosAutenticacionService, never()).limpiar(anyString(), anyString());
    }

    @Test
    void login_ShouldReturnPendiente2fa_WhenDosFactoresHabilitado() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setCorreo("juan@example.com");
        loginRequest.setContrasena("Password123!");

        when(usuarioRepository.resolverEstadoLogin("juan@example.com"))
                .thenReturn(estadoLoginJson(1L, "juan@example.com", true, "CLIENTE"));
        when(usuarioRepository.getReferenceById(1L)).thenReturn(usuario);
        when(preAuth2faTicketService.emitir(1L, "juan@example.com")).thenReturn("ticket-emitido");

        TokenResponse response = authService.login(loginRequest);

        assertTrue(response.isRequiere2fa());
        assertEquals("ticket-emitido", response.getPreAuthTicket());
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setCorreo("fantasma@example.com");
        loginRequest.setContrasena("cualquiera");

        when(usuarioRepository.resolverEstadoLogin("fantasma@example.com")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginRequest));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ── verify2Fa (§2.1 / OBS-AUTO-05) ──────────────────────────────────────

    @Test
    void verify2Fa_ShouldThrowUnauthorized_WhenTicketInvalido() {
        when(preAuth2faTicketService.resolver("ticket-invalido")).thenReturn(Optional.empty());

        TwoFactorRequest request = TwoFactorRequest.builder().codigo("123456").build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.verify2Fa("ticket-invalido", request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        // Ni siquiera debe consultar el correo del body: no existe (bypass cerrado).
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void verify2Fa_ShouldThrowUnauthorized_WhenCodeInvalid() {
        when(preAuth2faTicketService.resolver("ticket-valido"))
                .thenReturn(Optional.of(new PreAuth2faTicketService.DatosTicket(1L, "juan@example.com")));
        when(usuarioRepository.resolverEstadoLogin("juan@example.com"))
                .thenReturn(estadoLoginJson(1L, "juan@example.com", true, "CLIENTE"));
        when(twoFactorService.validarCodigoOBackup("juan@example.com", "000000")).thenReturn(false);

        TwoFactorRequest request = TwoFactorRequest.builder().codigo("000000").build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.verify2Fa("ticket-valido", request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        // El ticket NO se consume en un intento fallido (sigue disponible para reintentar).
        verify(preAuth2faTicketService, never()).consumir(anyString());
    }

    @Test
    void verify2Fa_ShouldSucceed_WhenTicketAndCodeValid() {
        when(preAuth2faTicketService.resolver("ticket-valido"))
                .thenReturn(Optional.of(new PreAuth2faTicketService.DatosTicket(1L, "juan@example.com")));
        when(usuarioRepository.resolverEstadoLogin("juan@example.com"))
                .thenReturn(estadoLoginJson(1L, "juan@example.com", true, "CLIENTE"));
        when(usuarioRepository.getReferenceById(1L)).thenReturn(usuario);
        when(twoFactorService.validarCodigoOBackup("juan@example.com", "123456")).thenReturn(true);
        when(preAuth2faTicketService.consumir("ticket-valido")).thenReturn(true);

        UserDetails userDetails = new User("juan@example.com", "hashed", List.of(new SimpleGrantedAuthority("CLIENTE")));
        when(userDetailsService.loadUserByUsername("juan@example.com")).thenReturn(userDetails);
        when(jwtService.generarToken(userDetails)).thenReturn("access-token");
        when(jwtService.generarRefreshToken(userDetails)).thenReturn("refresh-token");

        TwoFactorRequest request = TwoFactorRequest.builder().codigo("123456").build();
        TokenResponse response = authService.verify2Fa("ticket-valido", request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertFalse(response.isRequiere2fa());
    }

    @Test
    void verify2Fa_ShouldThrowUnauthorized_WhenTicketAlreadyConsumedConcurrently() {
        when(preAuth2faTicketService.resolver("ticket-valido"))
                .thenReturn(Optional.of(new PreAuth2faTicketService.DatosTicket(1L, "juan@example.com")));
        when(usuarioRepository.resolverEstadoLogin("juan@example.com"))
                .thenReturn(estadoLoginJson(1L, "juan@example.com", true, "CLIENTE"));
        when(twoFactorService.validarCodigoOBackup("juan@example.com", "123456")).thenReturn(true);
        when(preAuth2faTicketService.consumir("ticket-valido")).thenReturn(false); // otra petición lo consumió antes

        TwoFactorRequest request = TwoFactorRequest.builder().codigo("123456").build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.verify2Fa("ticket-valido", request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(jwtService);
    }

    @Test
    void verify2Fa_ShouldThrowBadRequest_When2faNoHabilitado() {
        when(preAuth2faTicketService.resolver("ticket-valido"))
                .thenReturn(Optional.of(new PreAuth2faTicketService.DatosTicket(1L, "juan@example.com")));
        when(usuarioRepository.resolverEstadoLogin("juan@example.com"))
                .thenReturn(estadoLoginJson(1L, "juan@example.com", false, "CLIENTE"));

        TwoFactorRequest request = TwoFactorRequest.builder().codigo("123456").build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.verify2Fa("ticket-valido", request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void verify2Fa_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        when(preAuth2faTicketService.resolver("ticket-valido"))
                .thenReturn(Optional.of(new PreAuth2faTicketService.DatosTicket(99L, "fantasma@example.com")));
        when(usuarioRepository.resolverEstadoLogin("fantasma@example.com")).thenReturn(null);

        TwoFactorRequest request = TwoFactorRequest.builder().codigo("123456").build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.verify2Fa("ticket-valido", request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ── refreshToken ─────────────────────────────────────────────────────────

    @Test
    void refreshToken_ShouldThrowUnauthorized_WhenTokenBlank() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken(""));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void refreshToken_ShouldThrowUnauthorized_WhenSesionNoEncontrada() {
        when(jwtService.extraerJti("refresh-token")).thenReturn("jti-1");
        when(sesionUsuarioRepository.findByJti("jti-1")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken("refresh-token"));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void refreshToken_ShouldThrowUnauthorized_WhenJtiNulo() {
        when(jwtService.extraerJti("refresh-token")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken("refresh-token"));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void refreshToken_ShouldThrowUnauthorized_WhenTokenInvalido() {
        when(jwtService.extraerJti("refresh-token")).thenReturn("jti-1");
        when(sesionUsuarioRepository.findByJti("jti-1")).thenReturn(Optional.of(uteq.edu.ec.artisync.entity.seguridad.SesionUsuario.builder().build()));
        when(jwtService.extraerUsername("refresh-token")).thenReturn("juan@example.com");
        UserDetails userDetails = new User("juan@example.com", "hashed", List.of(new SimpleGrantedAuthority("CLIENTE")));
        when(userDetailsService.loadUserByUsername("juan@example.com")).thenReturn(userDetails);
        when(jwtService.esRefreshTokenValido("refresh-token", userDetails)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken("refresh-token"));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void refreshToken_ShouldThrowForbidden_WhenCuentaInactiva() {
        Usuario inactivo = Usuario.builder().idUsuario(1L).correo("juan@example.com").estadoCuenta(false).build();

        when(jwtService.extraerJti("refresh-token")).thenReturn("jti-1");
        when(sesionUsuarioRepository.findByJti("jti-1")).thenReturn(Optional.of(uteq.edu.ec.artisync.entity.seguridad.SesionUsuario.builder().build()));
        when(jwtService.extraerUsername("refresh-token")).thenReturn("juan@example.com");
        UserDetails userDetails = new User("juan@example.com", "hashed", List.of(new SimpleGrantedAuthority("CLIENTE")));
        when(userDetailsService.loadUserByUsername("juan@example.com")).thenReturn(userDetails);
        when(jwtService.esRefreshTokenValido("refresh-token", userDetails)).thenReturn(true);
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(inactivo));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken("refresh-token"));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void refreshToken_ShouldSucceed_WhenTodoValido() {
        when(jwtService.extraerJti("refresh-token")).thenReturn("jti-1");
        when(sesionUsuarioRepository.findByJti("jti-1")).thenReturn(Optional.of(uteq.edu.ec.artisync.entity.seguridad.SesionUsuario.builder().build()));
        when(jwtService.extraerUsername("refresh-token")).thenReturn("juan@example.com");
        UserDetails userDetails = new User("juan@example.com", "hashed", List.of(new SimpleGrantedAuthority("CLIENTE")));
        when(userDetailsService.loadUserByUsername("juan@example.com")).thenReturn(userDetails);
        when(jwtService.esRefreshTokenValido("refresh-token", userDetails)).thenReturn(true);
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generarToken(userDetails)).thenReturn("nuevo-access");
        when(jwtService.generarRefreshToken(userDetails)).thenReturn("nuevo-refresh");
        when(jwtService.extraerJti("nuevo-access")).thenReturn("jti-access");
        when(jwtService.extraerJti("nuevo-refresh")).thenReturn("jti-refresh");
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(UsuarioRol.builder().rol(rolCliente).build()));

        TokenResponse response = authService.refreshToken("refresh-token");

        assertEquals("nuevo-access", response.getAccessToken());
        verify(sessionRevocationService).revocarToken("refresh-token");
        verify(sesionUsuarioRepository).deleteByJti("jti-1");
    }

    @Test
    void refreshToken_ShouldWrapUnexpectedException_AsUnauthorized() {
        when(jwtService.extraerJti("token-raro")).thenThrow(new RuntimeException("token malformado"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken("token-raro"));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    // ── logout ───────────────────────────────────────────────────────────────

    @Test
    void logout_ShouldRevocarAmbosTokensYBorrarSesion() {
        when(jwtService.extraerJti("refresh-token")).thenReturn("jti-1");

        RespuestaMensaje respuesta = authService.logout("Bearer access-token", "refresh-token");

        assertNotNull(respuesta);
        verify(sessionRevocationService).revocarTokenPorCabecera("Bearer access-token");
        verify(sessionRevocationService).revocarToken("refresh-token");
        verify(sesionUsuarioRepository).deleteByJti("jti-1");
    }

    @Test
    void logout_ShouldIgnoreRefreshToken_WhenBlank() {
        RespuestaMensaje respuesta = authService.logout("Bearer access-token", "");

        assertNotNull(respuesta);
        verify(sessionRevocationService, never()).revocarToken(anyString());
    }

    @Test
    void logout_ShouldNotPropagate_WhenJtiExtractionFails() {
        when(jwtService.extraerJti("refresh-token")).thenThrow(new RuntimeException("expirado"));

        RespuestaMensaje respuesta = assertDoesNotThrow(() -> authService.logout("Bearer access-token", "refresh-token"));

        assertNotNull(respuesta);
        verify(sesionUsuarioRepository, never()).deleteByJti(anyString());
    }

    // ── forgotPassword ───────────────────────────────────────────────────────

    @Test
    void forgotPassword_ShouldSendEmail_WhenUsuarioExiste() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().correo("juan@example.com").build();
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));

        RespuestaMensaje respuesta = authService.forgotPassword(request);

        assertNotNull(respuesta);
        verify(tokenRecuperacionRepository).save(any(TokenRecuperacion.class));
        verify(emailService).enviarCorreoRecuperacion(eq("juan@example.com"), eq("Juan"), anyString());
    }

    @Test
    void forgotPassword_ShouldRespondSameMessage_WhenUsuarioNoExiste() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().correo("fantasma@example.com").build();
        when(usuarioRepository.findByCorreo("fantasma@example.com")).thenReturn(Optional.empty());

        RespuestaMensaje respuesta = authService.forgotPassword(request);

        assertNotNull(respuesta);
        verifyNoInteractions(tokenRecuperacionRepository);
        verifyNoInteractions(emailService);
    }

    // ── resetPassword (REQ-F-005 / fn_restablecer_contrasena) ───────────────

    @Test
    void resetPassword_ShouldUpdatePassword_WhenTokenValido() {
        ResetPasswordRequest request = ResetPasswordRequest.builder().token("token-plano").nuevaContrasena("NuevaClave123!").build();
        when(passwordEncoder.encode("NuevaClave123!")).thenReturn("nuevo-hash");
        when(usuarioRepository.restablecerContrasena(anyString(), eq("nuevo-hash"))).thenReturn(1L);

        RespuestaMensaje respuesta = authService.resetPassword(request);

        assertNotNull(respuesta);
        verify(usuarioRepository).restablecerContrasena(anyString(), eq("nuevo-hash"));
    }

    @Test
    void resetPassword_ShouldThrowBadRequest_WhenTokenNoExiste() {
        ResetPasswordRequest request = ResetPasswordRequest.builder().token("token-invalido").nuevaContrasena("NuevaClave123!").build();
        when(passwordEncoder.encode(anyString())).thenReturn("nuevo-hash");
        when(usuarioRepository.restablecerContrasena(anyString(), anyString()))
                .thenThrow(excepcionSql("23514", "Este enlace ya ha sido utilizado o ha expirado"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void resetPassword_ShouldThrowBadRequest_WhenTokenExpirado() {
        ResetPasswordRequest request = ResetPasswordRequest.builder().token("token-viejo").nuevaContrasena("NuevaClave123!").build();
        when(passwordEncoder.encode(anyString())).thenReturn("nuevo-hash");
        when(usuarioRepository.restablecerContrasena(anyString(), anyString()))
                .thenThrow(excepcionSql("23514", "Este enlace ya ha sido utilizado o ha expirado"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
