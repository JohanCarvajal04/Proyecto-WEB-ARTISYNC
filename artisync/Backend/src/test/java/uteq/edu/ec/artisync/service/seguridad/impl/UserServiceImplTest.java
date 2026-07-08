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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateUserRequest;
import uteq.edu.ec.artisync.dto.shared.MessageResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.entity.seguridad.Pais;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.seguridad.PaisRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.UsuarioMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PaisRepository paisRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsuarioMapper usuarioMapper;
    @Mock
    private SessionRevocationService sessionRevocationService;

    @InjectMocks
    private UserServiceImpl userService;

    private Usuario usuario;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .idUsuario(1L)
                .correo("user@example.com")
                .nombres("Ana")
                .apellidos("Gomez")
                .contrasenaHash("hash")
                .build();

        userResponse = UserResponse.builder()
                .idUsuario(1L)
                .correo("user@example.com")
                .nombres("Ana")
                .apellidos("Gomez")
                .build();
    }

    @Test
    void getCurrentUser_ShouldReturnProfile_WhenUserExists() {
        when(usuarioRepository.findByCorreo("user@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        UserResponse result = userService.getCurrentUser("user@example.com");

        assertNotNull(result);
        assertEquals("Ana", result.getNombres());
        assertEquals("user@example.com", result.getCorreo());
    }

    @Test
    void getCurrentUser_ShouldThrowNotFound_WhenUserDoesNotExist() {
        when(usuarioRepository.findByCorreo("notfound@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> userService.getCurrentUser("notfound@example.com"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updateCurrentUser_ShouldUpdateNamesAndCountry() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setNombres("Ana Maria");
        request.setIdPais(5L);

        Pais pais = Pais.builder().idPais(5L).nombrePais("Ecuador").build();

        when(usuarioRepository.findByCorreo("user@example.com")).thenReturn(Optional.of(usuario));
        when(paisRepository.findById(5L)).thenReturn(Optional.of(pais));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        UserResponse result = userService.updateCurrentUser("user@example.com", request);

        assertNotNull(result);
        verify(usuarioRepository).save(usuario);
        assertEquals("Ana Maria", usuario.getNombres());
        assertEquals(pais, usuario.getPais());
    }

    @Test
    void deleteOwnAccount_ShouldRevokeSessionsAndDeleteUser() {
        when(usuarioRepository.findByCorreo("user@example.com")).thenReturn(Optional.of(usuario));

        MessageResponse response = userService.deleteOwnAccount("user@example.com");

        assertNotNull(response);
        assertEquals("Cuenta desactivada exitosamente", response.getMensaje());
        verify(sessionRevocationService).revocarSesionesUsuario(1L);
        verify(usuarioRepository).save(usuario);
        assertFalse(usuario.getEstadoCuenta());
    }
}
