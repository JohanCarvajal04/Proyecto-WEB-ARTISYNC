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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.seguridad.request.AdminUpdateUserRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.AssignRolesRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.ChangeEstadoRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.CreateUserRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.entity.seguridad.AutenticacionDosFactores;
import uteq.edu.ec.artisync.entity.seguridad.Pais;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.pedido.*;
import uteq.edu.ec.artisync.repository.legal.*;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.social.*;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.UsuarioMapper;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;
    @Mock
    private CodigoRespaldo2FaRepository codigoRespaldo2FaRepository;

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

    @Test
    void changeEstado_ShouldNotRevokeSessions_WhenActivatingUser() {
        Usuario inactivo = Usuario.builder().idUsuario(1L).correo("admin@example.com").estadoCuenta(false).build();
        ChangeEstadoRequest request = new ChangeEstadoRequest();
        request.setEstadoCuenta(true);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(inactivo));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(inactivo);
        when(usuarioMapper.toUserResponse(inactivo)).thenReturn(userResponse);

        adminUserService.changeEstado(1L, request);

        verify(sessionRevocationService, never()).revocarSesionesUsuario(any());
    }

    @Test
    void changeEstado_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        ChangeEstadoRequest request = new ChangeEstadoRequest();
        request.setEstadoCuenta(false);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserService.changeEstado(99L, request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ── createUser ───────────────────────────────────────────────────────────

    @Test
    void createUser_ShouldCreateWithDefaultRoleCliente() {
        CreateUserRequest request = CreateUserRequest.builder()
                .nombres("Nuevo").apellidos("Usuario").correo("nuevo@example.com")
                .contrasena("Password123!").build();
        Rol rolCliente = Rol.builder().idRol(1L).nombreRol("CLIENTE").build();

        when(usuarioRepository.existsByCorreo("nuevo@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(rolRepository.findByNombreRol("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        UserResponse result = adminUserService.createUser(request);

        assertNotNull(result);
        verify(usuarioRolRepository).save(any(UsuarioRol.class));
        verifyNoInteractions(perfilCreadorRepository);
    }

    @Test
    void createUser_ShouldRejectCorreoDuplicado() {
        CreateUserRequest request = CreateUserRequest.builder().correo("admin@example.com").build();
        when(usuarioRepository.existsByCorreo("admin@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserService.createUser(request));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void createUser_ShouldRejectPaisInexistente() {
        CreateUserRequest request = CreateUserRequest.builder().correo("nuevo@example.com").idPais(99L).build();
        when(usuarioRepository.existsByCorreo("nuevo@example.com")).thenReturn(false);
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserService.createUser(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createUser_ShouldCreatePerfilCreador_WhenRolCreadorAsignado() {
        CreateUserRequest request = CreateUserRequest.builder()
                .nombres("Nuevo").apellidos("Creador").correo("creador@example.com")
                .contrasena("Password123!").roles(List.of("CREADOR")).build();
        Rol rolCreador = Rol.builder().idRol(2L).nombreRol("CREADOR").build();

        when(usuarioRepository.existsByCorreo("creador@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(rolRepository.findByNombreRol("CREADOR")).thenReturn(Optional.of(rolCreador));
        when(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.empty());
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.createUser(request);

        verify(perfilCreadorRepository).save(any());
    }

    @Test
    void createUser_ShouldRejectRolInexistente() {
        CreateUserRequest request = CreateUserRequest.builder()
                .correo("nuevo@example.com").contrasena("Password123!").roles(List.of("FANTASMA")).build();
        when(usuarioRepository.existsByCorreo("nuevo@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(rolRepository.findByNombreRol("FANTASMA")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserService.createUser(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    // ── updateUser ───────────────────────────────────────────────────────────

    @Test
    void updateUser_ShouldUpdateCamposBasicos() {
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().nombres("Nuevo Nombre").build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        assertEquals("Nuevo Nombre", usuario.getNombres());
    }

    @Test
    void updateUser_ShouldClearPais_WhenIdPaisEsCero() {
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().idPais(0L).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        assertNull(usuario.getPais());
        verifyNoInteractions(paisRepository);
    }

    @Test
    void updateUser_ShouldAssignPais_WhenIdPaisValido() {
        Pais pais = Pais.builder().idPais(5L).nombrePais("Ecuador").build();
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().idPais(5L).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(paisRepository.findById(5L)).thenReturn(Optional.of(pais));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        assertEquals(pais, usuario.getPais());
    }

    @Test
    void updateUser_ShouldRejectPaisInexistente() {
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().idPais(99L).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserService.updateUser(1L, request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateUser_ShouldRevokeSessions_WhenDeactivating() {
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().estadoCuenta(false).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        verify(sessionRevocationService).revocarSesionesUsuario(1L);
    }

    @Test
    void updateUser_ShouldDisable2fa_WhenDosFactoresHabilitadoIsFalse() {
        AutenticacionDosFactores dosFactores = AutenticacionDosFactores.builder().estaHabilitado(true).build();
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().dosFactoresHabilitado(false).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(dosFactores));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        assertFalse(dosFactores.getEstaHabilitado());
        verify(codigoRespaldo2FaRepository).deleteByUsuarioIdUsuario(1L);
    }

    @Test
    void updateUser_ShouldUpdateRoles_WhenRolesProvided() {
        Rol rolCliente = Rol.builder().idRol(1L).nombreRol("CLIENTE").build();
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().roles(List.of("CLIENTE")).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of());
        when(rolRepository.findByNombreRol("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        verify(usuarioRolRepository).deleteAll(List.of());
        verify(usuarioRolRepository).save(any(UsuarioRol.class));
    }

    @Test
    void updateUser_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserService.updateUser(99L, AdminUpdateUserRequest.builder().build()));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ── assignRoles / deleteUser / revokeUserSessions ───────────────────────

    @Test
    void assignRoles_ShouldUpdateRolesAndRevokeSessions() {
        Rol rolCreador = Rol.builder().idRol(2L).nombreRol("CREADOR").build();
        AssignRolesRequest request = AssignRolesRequest.builder().roles(List.of("CREADOR")).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of());
        when(rolRepository.findByNombreRol("CREADOR")).thenReturn(Optional.of(rolCreador));
        when(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.empty());
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        UserResponse result = adminUserService.assignRoles(1L, request);

        assertNotNull(result);
        verify(sessionRevocationService).revocarSesionesUsuario(1L);
        verify(perfilCreadorRepository).save(any());
    }

    @Test
    void assignRoles_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        AssignRolesRequest request = AssignRolesRequest.builder().roles(List.of("CLIENTE")).build();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> adminUserService.assignRoles(99L, request));
    }

    @Test
    void deleteUser_ShouldDeactivateAndRevokeSessions() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        adminUserService.deleteUser(1L);

        assertFalse(usuario.getEstadoCuenta());
        verify(sessionRevocationService).revocarSesionesUsuario(1L);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deleteUser_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> adminUserService.deleteUser(99L));
    }

    @Test
    void revokeUserSessions_ShouldRevokeWhenUsuarioExiste() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);

        RespuestaMensaje respuesta = adminUserService.revokeUserSessions(1L);

        assertNotNull(respuesta);
        verify(sessionRevocationService).revocarSesionesUsuario(1L);
    }

    @Test
    void revokeUserSessions_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> adminUserService.revokeUserSessions(99L));
    }
}
