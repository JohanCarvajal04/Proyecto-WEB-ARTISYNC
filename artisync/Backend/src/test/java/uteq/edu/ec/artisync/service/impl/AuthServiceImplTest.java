package uteq.edu.ec.artisync.service.impl;

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
import uteq.edu.ec.artisync.dto.request.LoginRequest;
import uteq.edu.ec.artisync.dto.request.RegisterRequest;
import uteq.edu.ec.artisync.dto.response.TokenResponse;
import uteq.edu.ec.artisync.dto.response.UserResponse;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;
import uteq.edu.ec.artisync.repository.*;
import uteq.edu.ec.artisync.security.CustomUserDetailsService;
import uteq.edu.ec.artisync.security.JwtService;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    }
}
