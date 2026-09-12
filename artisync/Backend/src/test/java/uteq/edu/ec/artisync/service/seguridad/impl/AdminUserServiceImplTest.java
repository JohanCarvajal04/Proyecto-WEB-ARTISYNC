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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.peticion.seguridad.UserFilter;
import uteq.edu.ec.artisync.dto.seguridad.request.AdminUpdateUserRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.AssignRolesRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.ChangeEstadoRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.CreateUserRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.entity.seguridad.TwoFactorAuthentication;
import uteq.edu.ec.artisync.entity.seguridad.Country;
import uteq.edu.ec.artisync.entity.seguridad.Role;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.entity.seguridad.UserRole;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.pedido.*;
import uteq.edu.ec.artisync.repository.legal.*;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.social.*;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.UserMapper;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.IExportService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository usuarioRepository;
    @Mock
    private UserRoleRepository usuarioRolRepository;
    @Mock
    private CountryRepository paisRepository;
    @Mock
    private UserMapper usuarioMapper;
    @Mock
    private SessionRevocationService sessionRevocationService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TwoFactorAuthenticationRepository autenticacionDosFactoresRepository;
    @Mock
    private jakarta.persistence.EntityManager entityManager;
    @Mock
    private IExportService servicioExportacion;
    @Mock
    private uteq.edu.ec.artisync.service.shared.reporte.impl.ReportChartGenerator generadorGraficaReporte;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User usuario;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        usuario = User.builder()
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
        Page<User> page = new PageImpl<>(List.of(usuario));

        when(usuarioRepository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(page);
        when(usuarioMapper.toUserResponseList(List.of(usuario))).thenReturn(List.of(userResponse));

        PagedResponse<UserResponse> result = adminUserService.getAllUsers(new UserFilter(), pageRequest);

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

        UserResponse result = adminUserService.changeStatus(1L, request, 999L);

        assertNotNull(result);
        verify(sessionRevocationService).cambiarEstadoCuenta(1L, false);
        verify(entityManager).refresh(usuario);
        verify(usuarioRepository, never()).save(any());
        assertFalse(usuario.getEstadoCuenta());
    }

    @Test
    void changeEstado_ShouldNotRevokeSessions_WhenActivatingUser() {
        User inactivo = User.builder().idUsuario(1L).correo("admin@example.com").estadoCuenta(false).build();
        ChangeEstadoRequest request = new ChangeEstadoRequest();
        request.setEstadoCuenta(true);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(inactivo));
        when(usuarioMapper.toUserResponse(inactivo)).thenReturn(userResponse);

        adminUserService.changeStatus(1L, request, 999L);

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
                () -> adminUserService.changeStatus(99L, request, 999L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void changeEstado_ShouldThrowReglaNegocio_WhenAdminSeDesactivaASiMismo() {
        ChangeEstadoRequest request = new ChangeEstadoRequest();
        request.setEstadoCuenta(false);

        assertThrows(uteq.edu.ec.artisync.exception.BusinessRuleException.class,
                () -> adminUserService.changeStatus(1L, request, 1L));
        verify(usuarioRepository, never()).findById(any());
    }

    // ── createUser ───────────────────────────────────────────────────────────

    @Test
    // Fase 3 concurrencia: createUser delega en fn_crear_usuario_admin
    // (usuarioRepository.crearUsuarioAdmin), que captura unique_violation
    // sobre el correo en vez de existsByCorreo (A3), y compone con
    // fn_sincronizar_roles_usuario para los roles.
    void createUser_ShouldCreateWithDefaultRoleCliente() {
        CreateUserRequest request = CreateUserRequest.builder()
                .nombres("Nuevo").apellidos("User").correo("nuevo@example.com")
                .contrasena("Password123!").build();

        when(passwordEncoder.encode("Password123!")).thenReturn("hashed");
        when(usuarioRepository.crearUsuarioAdmin(eq("Nuevo"), eq("User"), eq("nuevo@example.com"), eq("hashed"),
                any(), isNull(), eq(true), eq(new String[]{"CLIENTE"}))).thenReturn(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        UserResponse result = adminUserService.createUser(request);

        assertNotNull(result);
        verify(usuarioRepository).crearUsuarioAdmin(eq("Nuevo"), eq("User"), eq("nuevo@example.com"), eq("hashed"),
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
                .thenThrow(excepcionSql("23503", "Country no encontrado"));

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
        when(usuarioRepository.save(any(User.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        assertEquals("Nuevo Nombre", usuario.getNombres());
    }

    @Test
    void updateUser_ShouldUpdateApellidosAndFechaNacimiento() {
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                .apellidos("Nuevos Apellidos")
                .fechaNacimiento(java.time.LocalDate.of(1990, 6, 15))
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(User.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        assertEquals("Nuevos Apellidos", usuario.getApellidos());
        assertEquals(java.time.LocalDate.of(1990, 6, 15), usuario.getFechaNacimiento());
    }

    @Test
    void updateUser_ShouldClearPais_WhenIdPaisEsCero() {
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().idPais(0L).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(User.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        assertNull(usuario.getPais());
        verifyNoInteractions(paisRepository);
    }

    @Test
    void updateUser_ShouldAssignPais_WhenIdPaisValido() {
        Country pais = Country.builder().idPais(5L).nombrePais("Ecuador").build();
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().idPais(5L).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(paisRepository.findById(5L)).thenReturn(Optional.of(pais));
        when(usuarioRepository.save(any(User.class))).thenReturn(usuario);
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
        when(usuarioRepository.save(any(User.class))).thenReturn(usuario);
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
        when(usuarioRepository.save(any(User.class))).thenReturn(usuario);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        adminUserService.updateUser(1L, request);

        verify(autenticacionDosFactoresRepository).desactivar2Fa(1L);
    }

    @Test
    void updateUser_ShouldUpdateRoles_WhenRolesProvided() {
        // Fase 1 concurrencia: updateRoles() delega en fn_sincronizar_roles_usuario
        // (una unica llamada atomica) en vez del find+deleteAll+bucle de save() anterior.
        AdminUpdateUserRequest request = AdminUpdateUserRequest.builder().roles(List.of("CLIENTE")).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.sincronizarRoles(1L, new String[]{"CLIENTE"})).thenReturn(1);
        when(usuarioRepository.save(any(User.class))).thenReturn(usuario);
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
        // Fase 1 concurrencia: updateRoles() delega en fn_sincronizar_roles_usuario
        // (incluye el alta perezosa de perfiles_creadores dentro del motor); ya no
        // hay llamadas a rolRepository/perfilCreadorRepository desde este metodo.
        AssignRolesRequest request = AssignRolesRequest.builder().roles(List.of("CREADOR")).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of());
        when(usuarioRolRepository.sincronizarRoles(1L, new String[]{"CREADOR"})).thenReturn(1);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        UserResponse result = adminUserService.assignRoles(1L, request, 999L);

        assertNotNull(result);
        verify(usuarioRolRepository).sincronizarRoles(1L, new String[]{"CREADOR"});
        verify(sessionRevocationService).revocarSesionesUsuario(1L);
    }

    @Test
    void assignRoles_ShouldCapturarRolesAnteriores_CuandoUsuarioYaTeniaRoles() {
        Role rolAnterior = Role.builder().idRol(3L).nombreRol("CLIENTE").build();
        UserRole usuarioRolAnterior = UserRole.builder().rol(rolAnterior).build();
        AssignRolesRequest request = AssignRolesRequest.builder().roles(List.of("CREADOR")).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(usuarioRolAnterior));
        when(usuarioRolRepository.sincronizarRoles(1L, new String[]{"CREADOR"})).thenReturn(1);
        when(usuarioMapper.toUserResponse(usuario)).thenReturn(userResponse);

        UserResponse result = adminUserService.assignRoles(1L, request, 999L);

        assertNotNull(result);
        verify(sessionRevocationService).revocarSesionesUsuario(1L);
    }

    @Test
    void assignRoles_ShouldThrowBadRequest_WhenRolInexistente() {
        AssignRolesRequest request = AssignRolesRequest.builder().roles(List.of("FANTASMA")).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of());
        when(usuarioRolRepository.sincronizarRoles(1L, new String[]{"FANTASMA"}))
                .thenThrow(excepcionSql("23503", "Uno o mas roles son inexistentes"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> adminUserService.assignRoles(1L, request, 999L));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(sessionRevocationService, never()).revocarSesionesUsuario(any());
    }

    @Test
    void assignRoles_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        AssignRolesRequest request = AssignRolesRequest.builder().roles(List.of("CLIENTE")).build();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> adminUserService.assignRoles(99L, request, 999L));
    }

    @Test
    void assignRoles_ShouldThrowReglaNegocio_WhenAdminSeCambiaSusPropiosRoles() {
        AssignRolesRequest request = AssignRolesRequest.builder().roles(List.of("CLIENTE")).build();

        assertThrows(uteq.edu.ec.artisync.exception.BusinessRuleException.class,
                () -> adminUserService.assignRoles(1L, request, 1L));
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void deleteUser_ShouldDeactivateAndRevokeSessions() {
        // Fase 1 concurrencia: deleteUser ya no carga la entidad completa ni
        // comprueba existencia por su cuenta; delega la desactivacion +
        // revocacion atomica (y la validacion de existencia, via P0002) en
        // fn_cambiar_estado_cuenta.
        adminUserService.deleteUser(1L, 999L);

        verify(sessionRevocationService).cambiarEstadoCuenta(1L, false);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deleteUser_ShouldThrowNotFound_WhenUsuarioNoExiste() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User no encontrado con ID: 99"))
                .when(sessionRevocationService).cambiarEstadoCuenta(99L, false);

        assertThrows(ResponseStatusException.class, () -> adminUserService.deleteUser(99L, 999L));
    }

    @Test
    void deleteUser_ShouldThrowReglaNegocio_WhenAdminSeEliminaASiMismo() {
        assertThrows(uteq.edu.ec.artisync.exception.BusinessRuleException.class,
                () -> adminUserService.deleteUser(1L, 1L));
        verify(sessionRevocationService, never()).cambiarEstadoCuenta(any(), org.mockito.ArgumentMatchers.anyBoolean());
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

    // ── export (USUARIO_EXPORTAR) ─────────────────────────────────────────

    @Test
    void exportar_ShouldThrowReglaNegocio_WhenExcedeTopeDeFilas() {
        when(usuarioRepository.count(any(Specification.class))).thenReturn((long) ReportFormat.CSV.topeFilas() + 1);

        UserFilter filtro = new UserFilter();
        assertThrows(uteq.edu.ec.artisync.exception.BusinessRuleException.class,
                () -> adminUserService.export(filtro, ReportFormat.CSV, "admin@artisync.dev"));
        verify(usuarioRepository, never()).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void exportar_ShouldConstruirModeloConFiltrosCompletos() {
        when(usuarioRepository.count(any(Specification.class))).thenReturn(1L);
        Page<User> pagina = new PageImpl<>(List.of(usuario));
        when(usuarioRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(pagina);
        when(usuarioMapper.toUserResponseList(List.of(usuario))).thenReturn(List.of(userResponse));
        GeneratedDocument esperado = new GeneratedDocument(new byte[]{1}, "text/csv", "usuarios.csv");
        when(servicioExportacion.exportar(any(ReportModel.class), org.mockito.ArgumentMatchers.eq(ReportFormat.CSV)))
                .thenReturn(esperado);

        UserFilter filtro = new UserFilter();
        filtro.setBusqueda("ana");
        filtro.setRol("ADMIN");
        filtro.setEstadoCuenta(true);

        GeneratedDocument resultado = adminUserService.export(filtro, ReportFormat.CSV, "admin@artisync.dev");

        assertSame(esperado, resultado);
        org.mockito.ArgumentCaptor<ReportModel> captor = org.mockito.ArgumentCaptor.forClass(ReportModel.class);
        verify(servicioExportacion).exportar(captor.capture(), org.mockito.ArgumentMatchers.eq(ReportFormat.CSV));
        ReportModel<UserResponse> modelo = captor.getValue();
        assertEquals(List.of(userResponse), modelo.getFilas());
        assertEquals("admin@artisync.dev", modelo.getGeneradoPor());
        assertEquals(Map.of("Búsqueda", "ana", "Role", "ADMIN", "Estado", "Activo"), modelo.getFiltrosAplicados());
    }

    @Test
    void exportar_conPaginacion_permiteExportarPorLotes() {
        Page<User> pagina = new PageImpl<>(List.of(usuario), PageRequest.of(0, 5000), 50_000);
        when(usuarioRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(pagina);
        when(usuarioMapper.toUserResponseList(List.of(usuario))).thenReturn(List.of(userResponse));
        GeneratedDocument esperado = new GeneratedDocument(new byte[]{1}, "application/pdf", "usuarios_parte_1.pdf");
        when(servicioExportacion.exportar(any(ReportModel.class), org.mockito.ArgumentMatchers.eq(ReportFormat.PDF)))
                .thenReturn(esperado);

        GeneratedDocument resultado = adminUserService.export(
                new UserFilter(), ReportFormat.PDF, uteq.edu.ec.artisync.service.shared.reporte.ReportChartType.NINGUNA, 0, 5000, "admin@artisync.dev");

        assertSame(esperado, resultado);
        org.mockito.ArgumentCaptor<ReportModel> captor = org.mockito.ArgumentCaptor.forClass(ReportModel.class);
        verify(servicioExportacion).exportar(captor.capture(), org.mockito.ArgumentMatchers.eq(ReportFormat.PDF));
        assertEquals("Usuarios - Parte 1", captor.getValue().getTitulo());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getSubtitulo()).contains("Parte 1 de 10");
    }

    @Test
    void exportar_ShouldReportarEstadoSuspendido_WhenEstadoCuentaEsFalse() {
        when(usuarioRepository.count(any(Specification.class))).thenReturn(1L);
        Page<User> pagina = new PageImpl<>(List.of(usuario));
        when(usuarioRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(pagina);
        when(usuarioMapper.toUserResponseList(List.of(usuario))).thenReturn(List.of(userResponse));
        when(servicioExportacion.exportar(any(ReportModel.class), org.mockito.ArgumentMatchers.eq(ReportFormat.CSV)))
                .thenReturn(new GeneratedDocument(new byte[]{1}, "text/csv", "usuarios.csv"));

        UserFilter filtro = new UserFilter();
        filtro.setEstadoCuenta(false);

        adminUserService.export(filtro, ReportFormat.CSV, "admin@artisync.dev");

        org.mockito.ArgumentCaptor<ReportModel> captor = org.mockito.ArgumentCaptor.forClass(ReportModel.class);
        verify(servicioExportacion).exportar(captor.capture(), org.mockito.ArgumentMatchers.eq(ReportFormat.CSV));
        assertEquals("Suspendido", captor.getValue().getFiltrosAplicados().get("Estado"));
    }

    @Test
    void exportar_ShouldConstruirModeloSinFiltros_WhenFiltroVacio() {
        when(usuarioRepository.count(any(Specification.class))).thenReturn(1L);
        Page<User> pagina = new PageImpl<>(List.of(usuario));
        when(usuarioRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(pagina);
        when(usuarioMapper.toUserResponseList(List.of(usuario))).thenReturn(List.of(userResponse));
        when(servicioExportacion.exportar(any(ReportModel.class), org.mockito.ArgumentMatchers.eq(ReportFormat.CSV)))
                .thenReturn(new GeneratedDocument(new byte[]{1}, "text/csv", "usuarios.csv"));

        adminUserService.export(new UserFilter(), ReportFormat.CSV, "admin@artisync.dev");

        org.mockito.ArgumentCaptor<ReportModel> captor = org.mockito.ArgumentCaptor.forClass(ReportModel.class);
        verify(servicioExportacion).exportar(captor.capture(), org.mockito.ArgumentMatchers.eq(ReportFormat.CSV));
        assertTrue(captor.getValue().getFiltrosAplicados().isEmpty());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("export con ReportChartType genera métricas y gráficas esperadas")
    void exportar_ConGraficas_GeneraModeloConGraficas() {
        when(usuarioRepository.count(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<User>>any())).thenReturn(2L);

        User u1 = User.builder().idUsuario(1L).correo("admin@test.com").estadoCuenta(true).build();
        User u2 = User.builder().idUsuario(2L).correo("creador@test.com").estadoCuenta(true).build();
        org.springframework.data.domain.Page<User> pagina = new org.springframework.data.domain.PageImpl<>(List.of(u1, u2));
        when(usuarioRepository.findAll(
                org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<User>>any(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(pagina);

        UserResponse r1 = UserResponse.builder().idUsuario(1L).correo("admin@test.com").estadoCuenta(true).roles(List.of("ADMIN")).nombrePais("Chile").build();
        UserResponse r2 = UserResponse.builder().idUsuario(2L).correo("creador@test.com").estadoCuenta(true).roles(List.of("CREADOR")).nombrePais("Colombia").build();
        when(usuarioMapper.toUserResponseList(any())).thenReturn(List.of(r1, r2));

        when(generadorGraficaReporte.generarGraficaRol(any())).thenReturn(new byte[]{1, 2, 3});
        when(generadorGraficaReporte.generarGraficaPais(any())).thenReturn(new byte[]{4, 5, 6});

        uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument esperado =
                new uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument(new byte[]{1}, "application/pdf", "usuarios.pdf");
        when(servicioExportacion.exportar(any(), any())).thenReturn(esperado);

        uteq.edu.ec.artisync.dto.peticion.seguridad.UserFilter filtro = new uteq.edu.ec.artisync.dto.peticion.seguridad.UserFilter();
        uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument resultado =
                adminUserService.export(filtro, uteq.edu.ec.artisync.service.shared.reporte.ReportFormat.PDF,
                        uteq.edu.ec.artisync.service.shared.reporte.ReportChartType.AMBAS, "admin@artisync.com");

        assertNotNull(resultado);
        verify(generadorGraficaReporte).generarGraficaRol(any());
        verify(generadorGraficaReporte).generarGraficaPais(any());
        verify(servicioExportacion).exportar(any(), org.mockito.ArgumentMatchers.eq(uteq.edu.ec.artisync.service.shared.reporte.ReportFormat.PDF));
    }
}
