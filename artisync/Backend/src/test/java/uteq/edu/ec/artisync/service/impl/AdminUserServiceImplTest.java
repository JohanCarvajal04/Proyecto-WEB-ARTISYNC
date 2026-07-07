package uteq.edu.ec.artisync.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.request.ChangeEstadoRequest;
import uteq.edu.ec.artisync.dto.response.UserResponse;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.*;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.UsuarioMapper;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private PaisRepository paisRepository;
    @Mock
    private PerfilCreadorRepository perfilCreadorRepository;
    @Mock
    private UsuarioMapper usuarioMapper;
    @Mock
    private SessionRevocationService sessionRevocationService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private Usuario usuario;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .idUsuario(1L)
                .correo("admin@example.com")
                .nombres("Admin")
                .apellidos("User")
                .estadoCuenta(true)
                .build();

        userResponse = UserResponse.builder()
                .idUsuario(1L)
                .correo("admin@example.com")
                .nombres("Admin")
                .apellidos("User")
                .estadoCuenta(true)
                .build();
    }

    @Test
    void getAllUsers_ShouldReturnPagedResponse() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Usuario> page = new PageImpl<>(List.of(usuario));

        when(usuarioRepository.findAll(pageRequest)).thenReturn(page);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        PagedResponse<UserResponse> result = adminUserService.getAllUsers(pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Admin", result.getContent().get(0).getNombres());
    }

    @Test
    void getUserById_ShouldReturnUser_WhenExists() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        UserResponse result = adminUserService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getIdUsuario());
    }

    @Test
    void getUserById_ShouldThrowNotFound_WhenDoesNotExist() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> adminUserService.getUserById(99L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void changeEstado_ShouldRevokeSessions_WhenDeactivatingUser() {
        ChangeEstadoRequest request = new ChangeEstadoRequest();
        request.setEstadoCuenta(false);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        UserResponse result = adminUserService.changeEstado(1L, request);

        assertNotNull(result);
        verify(sessionRevocationService).revocarSesionesUsuario(1L);
        verify(usuarioRepository).save(usuario);
    }
}
