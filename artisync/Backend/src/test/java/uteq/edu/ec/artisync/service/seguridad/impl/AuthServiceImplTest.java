package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.controller.seguridad.*;
import uteq.edu.ec.artisync.service.seguridad.*;
import uteq.edu.ec.artisync.service.seguridad.impl.*;
import uteq.edu.ec.artisync.service.shared.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import uteq.edu.ec.artisync.entity.seguridad.AutenticacionDosFactores;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.entity.seguridad.TokenRecuperacion;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.pedido.*;
import uteq.edu.ec.artisync.repository.legal.*;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.social.*;
import uteq.edu.ec.artisync.security.CustomUserDetailsService;
import uteq.edu.ec.artisync.security.JwtService;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;

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
    private RolRepository rolRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private PerfilCreadorRepository perfilCreadorRepository;
    @Mock
    private AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;
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

    @Test
    void register_ShouldThrowException_WhenUserIsMinorRNF12() {
        registerRequest.setFechaNacimiento(LocalDate.now().minusYears(15));
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.register(registerRequest));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("18 años"));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        when(usuarioRepository.existsByCorreo("juan@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.register(registerRequest));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void register_ShouldRegisterUserSuccessfully() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(rolRepository.findByNombreRol("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(usuarioRolRepository.save(any(UsuarioRol.class))).thenReturn(new UsuarioRol());

        UserResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals(1L, response.getIdUsuario());
        assertEquals("juan@example.com", response.getCorreo());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    void login_ShouldLoginSuccessfully() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setCorreo("juan@example.com");
        loginRequest.setContrasena("Password123!");

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.empty());

        UserDetails userDetails = new User("juan@example.com", "hashed", List.of(new SimpleGrantedAuthority("CLIENTE")));
        when(userDetailsService.loadUserByUsername("juan@example.com")).thenReturn(userDetails);
        when(jwtService.generarToken(userDetails)).thenReturn("access-token");
        when(jwtService.generarRefreshToken(userDetails)).thenReturn("refresh-token");
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(UsuarioRol.builder().rol(rolCliente).build()));

        TokenResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertFalse(response.isRequiere2fa());
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

    // ── verify2Fa (§2.1 / OBS-AUTO-05) ──────────────────────────────────────

    @Test
    void verify2Fa_ShouldThrowUnauthorized_WhenTicketInvalid() {
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
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L))
                .thenReturn(Optional.of(AutenticacionDosFactores.builder().estaHabilitado(true).build()));
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
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L))
                .thenReturn(Optional.of(AutenticacionDosFactores.builder().estaHabilitado(true).build()));
        when(twoFactorService.validarCodigoOBackup("juan@example.com", "123456")).thenReturn(true);
        when(preAuth2faTicketService.consumir("ticket-valido")).thenReturn(true);

        UserDetails userDetails = new User("juan@example.com", "hashed", List.of(new SimpleGrantedAuthority("CLIENTE")));
        when(userDetailsService.loadUserByUsername("juan@example.com")).thenReturn(userDetails);
        when(jwtService.generarToken(userDetails)).thenReturn("access-token");
        when(jwtService.generarRefreshToken(userDetails)).thenReturn("refresh-token");
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(UsuarioRol.builder().rol(rolCliente).build()));

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
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L))
                .thenReturn(Optional.of(AutenticacionDosFactores.builder().estaHabilitado(true).build()));
        when(twoFactorService.validarCodigoOBackup("juan@example.com", "123456")).thenReturn(true);
        when(preAuth2faTicketService.consumir("ticket-valido")).thenReturn(false); // otra petición lo consumió antes

        TwoFactorRequest request = TwoFactorRequest.builder().codigo("123456").build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.verify2Fa("ticket-valido", request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(jwtService);
    }

    @Test
    void verify2Fa_ShouldThrowBadRequest_When2faNoConfigurado() {
        when(preAuth2faTicketService.resolver("ticket-valido"))
                .thenReturn(Optional.of(new PreAuth2faTicketService.DatosTicket(1L, "juan@example.com")));
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.empty());

        TwoFactorRequest request = TwoFactorRequest.builder().codigo("123456").build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.verify2Fa("ticket-valido", request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void verify2Fa_ShouldThrowBadRequest_WhenRegistroExisteYaDeshabilitado() {
        when(preAuth2faTicketService.resolver("ticket-valido"))
                .thenReturn(Optional.of(new PreAuth2faTicketService.DatosTicket(1L, "juan@example.com")));
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L))
                .thenReturn(Optional.of(AutenticacionDosFactores.builder().estaHabilitado(false).build()));

        TwoFactorRequest request = TwoFactorRequest.builder().codigo("123456").build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.verify2Fa("ticket-valido", request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void verify2Fa_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        when(preAuth2faTicketService.resolver("ticket-valido"))
                .thenReturn(Optional.of(new PreAuth2faTicketService.DatosTicket(99L, "fantasma@example.com")));
        when(usuarioRepository.findByCorreo("fantasma@example.com")).thenReturn(Optional.empty());

        TwoFactorRequest request = TwoFactorRequest.builder().codigo("123456").build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.verify2Fa("ticket-valido", request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ── register: ramas adicionales ─────────────────────────────────────────

    @Test
    void register_ShouldCreatePerfilCreador_WhenRolIsCreador() {
        registerRequest.setRol("CREADOR");
        Rol rolCreador = Rol.builder().idRol(2L).nombreRol("CREADOR").build();

        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(rolRepository.findByNombreRol("CREADOR")).thenReturn(Optional.of(rolCreador));
        when(usuarioRolRepository.save(any(UsuarioRol.class))).thenReturn(new UsuarioRol());

        UserResponse response = authService.register(registerRequest);

        assertEquals(List.of("CREADOR"), response.getRoles());
        verify(perfilCreadorRepository).save(any());
    }

    @Test
    void register_ShouldDefaultToCliente_WhenRolIsBlank() {
        registerRequest.setRol("");
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(rolRepository.findByNombreRol("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(usuarioRolRepository.save(any(UsuarioRol.class))).thenReturn(new UsuarioRol());

        UserResponse response = authService.register(registerRequest);

        assertEquals(List.of("CLIENTE"), response.getRoles());
        verifyNoInteractions(perfilCreadorRepository);
    }

    @Test
    void register_ShouldRejectRolNoPermitido() {
        registerRequest.setRol("ADMIN");
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(registerRequest));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(usuarioRolRepository);
    }

    @Test
    void register_ShouldThrowBadRequest_WhenRolNoExisteEnBD() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(rolRepository.findByNombreRol("CLIENTE")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(registerRequest));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    // ── login: rama pendiente de 2FA ────────────────────────────────────────

    @Test
    void login_ShouldReturnPendiente2fa_WhenDosFactoresHabilitado() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setCorreo("juan@example.com");
        loginRequest.setContrasena("Password123!");

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L))
                .thenReturn(Optional.of(AutenticacionDosFactores.builder().estaHabilitado(true).build()));
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

        when(usuarioRepository.findByCorreo("fantasma@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginRequest));
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

    // ── forgotPassword / resetPassword ──────────────────────────────────────

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

    @Test
    void resetPassword_ShouldUpdatePassword_WhenTokenValido() {
        ResetPasswordRequest request = ResetPasswordRequest.builder().token("token-plano").nuevaContrasena("NuevaClave123!").build();
        TokenRecuperacion tokenRec = TokenRecuperacion.builder()
                .usuario(usuario).usado(false).fechaGeneracion(java.time.LocalDateTime.now()).build();

        when(tokenRecuperacionRepository.findByHashTokenAndUsadoFalse(anyString())).thenReturn(Optional.of(tokenRec));
        when(passwordEncoder.encode("NuevaClave123!")).thenReturn("nuevo-hash");

        RespuestaMensaje respuesta = authService.resetPassword(request);

        assertNotNull(respuesta);
        assertEquals("nuevo-hash", usuario.getContrasenaHash());
        assertTrue(tokenRec.getUsado());
        verify(usuarioRepository).save(usuario);
        verify(tokenRecuperacionRepository).save(tokenRec);
    }

    @Test
    void resetPassword_ShouldThrowBadRequest_WhenTokenNoExiste() {
        ResetPasswordRequest request = ResetPasswordRequest.builder().token("token-invalido").nuevaContrasena("NuevaClave123!").build();
        when(tokenRecuperacionRepository.findByHashTokenAndUsadoFalse(anyString())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void resetPassword_ShouldThrowBadRequest_WhenTokenExpirado() {
        ResetPasswordRequest request = ResetPasswordRequest.builder().token("token-viejo").nuevaContrasena("NuevaClave123!").build();
        TokenRecuperacion tokenRec = TokenRecuperacion.builder()
                .usuario(usuario).usado(false).fechaGeneracion(java.time.LocalDateTime.now().minusHours(2)).build();

        when(tokenRecuperacionRepository.findByHashTokenAndUsadoFalse(anyString())).thenReturn(Optional.of(tokenRec));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(usuarioRepository, never()).save(any());
    }
}
