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
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private PaisRepository paisRepository;
    @Mock
    private UsuarioMapper usuarioMapper;
    @Mock
    private SessionRevocationService sessionRevocationService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;
    @Mock
    private jakarta.persistence.EntityManager entityManager;

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

    /** Simula lo que Spring Data envuelve cuando fn_x lanza RAISE EXCEPTION ... USING ERRCODE = '...'. */
    private static RuntimeException excepcionSql(String sqlState, String mensaje) {
        return new RuntimeException(new java.sql.SQLException(mensaje, sqlState));
    }

    @Test
    void getAllUsers_ShouldReturnPagedResponse() {
        // Fase 2 rendimiento: getAllUsers mapea la pagina en un solo lote via
        // toUserResponseList (batchea roles/permisos/2FA), no fila a fila con
        // toUserResponse.
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Usuario> page = new PageImpl<>(List.of(usuario));

        when(usuarioRepository.findAll(pageRequest)).thenReturn(page);
        when(usuarioMapper.toUserResponseList(List.of(usuario))).thenReturn(List.of(userResponse));

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
        // Fase 1 concurrencia: fn_cambiar_estado_cuenta decide internamente (bajo
        // SELECT FOR UPDATE) si hubo transicion activa->inactiva y revoca
        // sesiones; el servicio ya no compara un estadoAnterior en Java ni llama
        // a usuarioRepository.save() por separado.
        //
        // entityManager.refresh() (no un usuario.setEstadoCuenta() explicito) es
        // quien deja el campo en memoria coherente con lo que la funcion atomica
        // ya escribio -- se simula aqui exactamente como lo haria Hibernate real.
        ChangeEstadoRequest request = new ChangeEstadoRequest();
        request.setEstadoCuenta(false);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);
        doAnswer(inv -> {
            usuario.setEstadoCuenta(false);
            return null;
        }).when(entityManager).refresh(usuario);

        UserResponse result = adminUserService.changeEstado(1L, request);

        assertNotNull(result);
        verify(sessionRevocationService).cambiarEstadoCuenta(1L, false);
        verify(entityManager).refresh(usuario);
        verify(usuarioRepository, never()).save(any());
        assertFalse(usuario.getEstadoCuenta());
    }

    @Test
    void changeEstado_ShouldNotRevokeSessions_WhenActivatingUser() {
        Usuario inactivo = Usuario.builder().idUsuario(1L).correo("admin@example.com").estadoCuenta(false).build();
        ChangeEstadoRequest request = new ChangeEstadoRequest();
        request.setEstadoCuenta(true);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(inactivo));
        when(usuarioMapper.toUserResponse(inactivo)).thenReturn(userResponse);

        adminUserService.changeEstado(1L, request);

        // La decision de revocar (o no) ahora vive dentro de fn_cambiar_estado_cuenta;
        // el servicio siempre delega, sin ramificar en Java.
        verify(sessionRevocationService).cambiarEstadoCuenta(1L, true);
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
    // Fase 3 concurrencia: createUser delega en fn_crear_usuario_admin
    // (usuarioRepository.crearUsuarioAdmin), que captura unique_violation
    // sobre el correo en vez de existsByCorreo (A3), y compone con
    // fn_sincronizar_roles_usuario para los roles.
    void createUser_ShouldCreateWithDefaultRoleCliente() {
        CreateUserRequest request = CreateUserRequest.builder()
                .nombres("Nuevo").apellidos("Usuario").correo("nuevo@example.com")
                .contrasena("Password123!").build();

        when(passwordEncoder.encode("Password123!")).thenReturn("hashed");
        when(usuarioRepository.crearUsuarioAdmin(eq("Nuevo"), eq("Usuario"), eq("nuevo@example.com"), eq("hashed"),
                any(), isNull(), eq(true), eq(new String[]{"CLIENTE"}))).thenReturn(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        UserResponse result = adminUserService.createUser(request);

        assertNotNull(result);
        verify(usuarioRepository).crearUsuarioAdmin(eq("Nuevo"), eq("Usuario"), eq("nuevo@example.com"), eq("hashed"),
                any(), isNull(), eq(true), eq(new String[]{"CLIENTE"}));
    }

    @Test
    void createUser_ShouldRejectCorreoDuplicado() {
        // fn_crear_usuario_admin captura unique_violation (ERRCODE 23505)
        // sobre usuarios.correo en vez de una comprobacion existsByCorreo
        // previa no atomica (A3).
        CreateUserRequest request = CreateUserRequest.builder().correo("admin@example.com").contrasena("x").build();
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.crearUsuarioAdmin(any(), any(), eq("admin@example.com"), any(), any(), any(), any(), any()))
                .thenThrow(excepcionSql("23505", "El correo ya esta registrado: admin@example.com"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserService.createUser(request));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void createUser_ShouldRejectPaisInexistente() {
        CreateUserRequest request = CreateUserRequest.builder().correo("nuevo@example.com").idPais(99L).contrasena("x").build();
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.crearUsuarioAdmin(any(), any(), eq("nuevo@example.com"), any(), any(), eq(99L), any(), any()))
                .thenThrow(excepcionSql("23503", "Pais no encontrado"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserService.createUser(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createUser_ShouldCreatePerfilCreador_WhenRolCreadorAsignado() {
        // El alta perezosa del perfil de creador ahora ocurre DENTRO de
        // fn_sincronizar_roles_usuario (compuesta por fn_crear_usuario_admin);
        // este test verifica que el rol solicitado llegue tal cual a la
        // rutina, no que el servicio Java toque perfiles_creadores.
        CreateUserRequest request = CreateUserRequest.builder()
                .nombres("Nuevo").apellidos("Creador").correo("creador@example.com")
                .contrasena("Password123!").roles(List.of("CREADOR")).build();

        when(passwordEncoder.encode("Password123!")).thenReturn("hashed");
        when(usuarioRepository.crearUsuarioAdmin(any(), any(), eq("creador@example.com"), any(), any(), any(), any(),
                eq(new String[]{"CREADOR"}))).thenReturn(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.createUser(request);

        verify(usuarioRepository).crearUsuarioAdmin(any(), any(), eq("creador@example.com"), any(), any(), any(), any(),
                eq(new String[]{"CREADOR"}));
    }

    @Test
    void createUser_ShouldRejectRolInexistente() {
        // fn_sincronizar_roles_usuario (invocada dentro de fn_crear_usuario_admin)
        // lanza ERRCODE 23514 cuando un rol solicitado no existe.
        CreateUserRequest request = CreateUserRequest.builder()
                .correo("nuevo@example.com").contrasena("Password123!").roles(List.of("FANTASMA")).build();
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.crearUsuarioAdmin(any(), any(), eq("nuevo@example.com"), any(), any(), any(), any(),
                eq(new String[]{"FANTASMA"})))
                .thenThrow(excepcionSql("23514", "El rol especificado no existe en el sistema: FANTASMA"));

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
        // Fase 1 concurrencia: la rama estadoCuenta de updateUser delega en
        // fn_cambiar_estado_cuenta (SELECT FOR UPDATE + revocacion atomica) en
        // vez de comparar un estadoAnterior leido en Java.
        //
        // Ademas fija el hallazgo del code-review: estado_cuenta se persiste
        // UNA sola vez (via la funcion atomica) -- usuarioRepository.save() ya
        // no debe reescribirlo por dirty-checking. Se verifica con
        // times(1)/entityManager.refresh() en vez de un usuario.setEstadoCuenta()
        // que dejaria el campo "dirty" para el siguiente flush.
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().estadoCuenta(false).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        verify(sessionRevocationService).cambiarEstadoCuenta(1L, false);
        verify(usuarioRepository, times(1)).save(usuario);
        verify(entityManager).refresh(usuario);
    }

    @Test
    void updateUser_ShouldDisable2fa_WhenDosFactoresHabilitadoIsFalse() {
        // Fase 3 concurrencia: la rama dosFactoresHabilitado=false delega en
        // fn_desactivar_2fa (autenticacionDosFactoresRepository.desactivar2Fa),
        // que desactiva el flag y purga codigos de respaldo atomicamente.
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().dosFactoresHabilitado(false).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.desactivar2Fa(1L)).thenReturn(true);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        verify(autenticacionDosFactoresRepository).desactivar2Fa(1L);
    }

    @Test
    void updateUser_ShouldUpdateRoles_WhenRolesProvided() {
        // Fase 1 concurrencia: actualizarRoles() delega en fn_sincronizar_roles_usuario
        // (una unica llamada atomica) en vez del find+deleteAll+bucle de save() anterior.
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().roles(List.of("CLIENTE")).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.sincronizarRoles(1L, new String[]{"CLIENTE"})).thenReturn(1);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        verify(usuarioRolRepository).sincronizarRoles(1L, new String[]{"CLIENTE"});
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
        // Fase 1 concurrencia: actualizarRoles() delega en fn_sincronizar_roles_usuario
        // (incluye el alta perezosa de perfiles_creadores dentro del motor); ya no
        // hay llamadas a rolRepository/perfilCreadorRepository desde este metodo.
        AssignRolesRequest request = AssignRolesRequest.builder().roles(List.of("CREADOR")).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of());
        when(usuarioRolRepository.sincronizarRoles(1L, new String[]{"CREADOR"})).thenReturn(1);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        UserResponse result = adminUserService.assignRoles(1L, request);

        assertNotNull(result);
        verify(usuarioRolRepository).sincronizarRoles(1L, new String[]{"CREADOR"});
        verify(sessionRevocationService).revocarSesionesUsuario(1L);
    }

    @Test
    void assignRoles_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        AssignRolesRequest request = AssignRolesRequest.builder().roles(List.of("CLIENTE")).build();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> adminUserService.assignRoles(99L, request));
    }

    @Test
    void deleteUser_ShouldDeactivateAndRevokeSessions() {
        // Fase 1 concurrencia: deleteUser ya no carga la entidad completa; solo
        // comprueba existencia y delega la desactivacion + revocacion atomica en
        // fn_cambiar_estado_cuenta.
        when(usuarioRepository.existsById(1L)).thenReturn(true);

        adminUserService.deleteUser(1L);

        verify(sessionRevocationService).cambiarEstadoCuenta(1L, false);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deleteUser_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

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
