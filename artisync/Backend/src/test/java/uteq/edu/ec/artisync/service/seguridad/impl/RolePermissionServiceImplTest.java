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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.dto.seguridad.request.CreateRoleRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateRoleRequest;
import uteq.edu.ec.artisync.dto.seguridad.response.PermisoResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.RolResponse;
import uteq.edu.ec.artisync.entity.seguridad.Permiso;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.repository.seguridad.PermisoRepository;
import uteq.edu.ec.artisync.repository.seguridad.RolRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRolRepository;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceImplTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PermisoRepository permisoRepository;

    @Mock
    private UsuarioRolRepository usuarioRolRepository;

    @Mock
    private SessionRevocationService sessionRevocationService;

    @InjectMocks
    private RolePermissionServiceImpl service;

    private Rol rolCustom;

    @BeforeEach
    void setUp() {
        rolCustom = Rol.builder()
                .idRol(10L)
                .nombreRol("SUPERVISOR")
                .descripcionRol("Rol de supervisión")
                .permisos(new HashSet<>())
                .build();
    }

    /** Simula lo que Spring Data envuelve cuando fn_x lanza RAISE EXCEPTION ... USING ERRCODE = '...'. */
    private static RuntimeException excepcionSql(String sqlState, String mensaje) {
        return new RuntimeException(new SQLException(mensaje, sqlState));
    }

    // Fase 3 concurrencia: createRole delega en fn_crear_rol (rolRepository.crearRol),
    // que captura unique_violation en el motor en vez de una comprobacion
    // findByNombreRol previa no atomica (A8).
    @Test
    void createRole_Success() {
        CreateRoleRequest req = new CreateRoleRequest("SUPERVISOR", "Rol de supervisión", List.of());
        when(rolRepository.crearRol("SUPERVISOR", "Rol de supervisión", new String[0])).thenReturn(10L);
        when(rolRepository.findById(10L)).thenReturn(Optional.of(rolCustom));

        RolResponse res = service.createRole(req);

        assertNotNull(res);
        assertEquals("SUPERVISOR", res.getNombreRol());
        verify(rolRepository).crearRol("SUPERVISOR", "Rol de supervisión", new String[0]);
    }

    @Test
    void createRole_ConflictWhenRoleExists() {
        CreateRoleRequest req = new CreateRoleRequest("SUPERVISOR", "Rol de supervisión", List.of());
        when(rolRepository.crearRol("SUPERVISOR", "Rol de supervisión", new String[0]))
                .thenThrow(excepcionSql("23505", "Ya existe un rol con el nombre: SUPERVISOR"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createRole(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(rolRepository, never()).save(any(Rol.class));
    }

    @Test
    void updateRole_Success() {
        UpdateRoleRequest req = new UpdateRoleRequest("Nueva descripción");
        when(rolRepository.findById(10L)).thenReturn(Optional.of(rolCustom));
        when(rolRepository.save(any(Rol.class))).thenReturn(rolCustom);

        RolResponse res = service.updateRole(10L, req);

        assertNotNull(res);
        verify(rolRepository).save(any(Rol.class));
    }

    // ── deleteRole (REQ-F-004 / fn_eliminar_rol) ────────────────────────────

    @Test
    void deleteRole_Success() {
        when(rolRepository.eliminarRol(10L)).thenReturn(Boolean.TRUE);

        assertDoesNotThrow(() -> service.deleteRole(10L));
        verify(rolRepository).eliminarRol(10L);
    }

    @Test
    void deleteRole_FailsForSystemRole() {
        when(rolRepository.eliminarRol(1L))
                .thenThrow(excepcionSql("23514", "No se puede eliminar un rol base del sistema: ADMIN"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.deleteRole(1L));
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void deleteRole_FailsWhenUsuariosAsignados() {
        when(rolRepository.eliminarRol(10L))
                .thenThrow(excepcionSql("23514", "No se puede eliminar el rol porque tiene usuarios activos asignados"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.deleteRole(10L));
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void deleteRole_ThrowsNotFound_WhenRolNoExiste() {
        when(rolRepository.eliminarRol(99L))
                .thenThrow(excepcionSql("P0002", "Rol no encontrado con ID: 99"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.deleteRole(99L));
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getAllRoles_MapeaPermisos() {
        Permiso permiso = Permiso.builder().idPermiso(1L).nombrePermiso("CATALOGO_VER").build();
        rolCustom.setPermisos(new HashSet<>(Set.of(permiso)));
        when(rolRepository.findAll()).thenReturn(List.of(rolCustom));

        List<RolResponse> resultado = service.getAllRoles();

        assertEquals(1, resultado.size());
        assertEquals(List.of("CATALOGO_VER"), resultado.get(0).getPermisos());
    }

    @Test
    void getAllPermisos_Mapea() {
        Permiso permiso = Permiso.builder().idPermiso(1L).nombrePermiso("CATALOGO_VER").moduloAplicacion("catalogo").build();
        when(permisoRepository.findAll()).thenReturn(List.of(permiso));

        List<PermisoResponse> resultado = service.getAllPermisos();

        assertEquals(1, resultado.size());
        assertEquals("CATALOGO_VER", resultado.get(0).getNombrePermiso());
    }

    @Test
    void getPermissionsByRole_DevuelvePermisos() {
        Permiso permiso = Permiso.builder().idPermiso(1L).nombrePermiso("CATALOGO_VER").build();
        rolCustom.setPermisos(new HashSet<>(Set.of(permiso)));
        when(rolRepository.findByNombreRol("SUPERVISOR")).thenReturn(Optional.of(rolCustom));

        List<String> resultado = service.getPermissionsByRole("supervisor");

        assertEquals(List.of("CATALOGO_VER"), resultado);
    }

    @Test
    void getPermissionsByRole_ListaVaciaSinPermisos() {
        rolCustom.setPermisos(null);
        when(rolRepository.findByNombreRol("SUPERVISOR")).thenReturn(Optional.of(rolCustom));

        assertTrue(service.getPermissionsByRole("SUPERVISOR").isEmpty());
    }

    @Test
    void getPermissionsByRole_ThrowsNotFound_WhenRolNoExiste() {
        when(rolRepository.findByNombreRol("FANTASMA")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.getPermissionsByRole("fantasma"));
    }

    // ── syncPermissions (REQ-F-003 / fn_sincronizar_permisos_rol) ───────────

    @Test
    void syncPermissions_AsignaLosPermisosIndicados() {
        when(rolRepository.sincronizarPermisos(eq("SUPERVISOR"), any(String[].class))).thenReturn(1);

        assertDoesNotThrow(() -> service.syncPermissions("supervisor", List.of("catalogo_ver")));

        verify(rolRepository).sincronizarPermisos(eq("SUPERVISOR"), eq(new String[]{"catalogo_ver"}));
    }

    @Test
    void syncPermissions_LimpiaPermisos_CuandoListaVacia() {
        when(rolRepository.sincronizarPermisos(eq("SUPERVISOR"), any(String[].class))).thenReturn(0);

        assertDoesNotThrow(() -> service.syncPermissions("SUPERVISOR", List.of()));

        verify(rolRepository).sincronizarPermisos(eq("SUPERVISOR"), eq(new String[0]));
    }

    /**
     * Los permisos viajan en el claim `permisos` del JWT: si no se revocan las
     * sesiones, quien ya estuviera dentro seguiría con los permisos antiguos
     * (y con el menú anterior) hasta que caducara su token.
     */
    @Test
    void syncPermissions_RevocaLasSesionesDeLosUsuariosDelRol() {
        when(rolRepository.sincronizarPermisos(eq("SUPERVISOR"), any(String[].class))).thenReturn(1);
        when(usuarioRolRepository.findIdsUsuarioByNombreRol("SUPERVISOR")).thenReturn(List.of(7L, 9L));

        service.syncPermissions("supervisor", List.of("USUARIO_VER"));

        verify(sessionRevocationService).revocarSesionesUsuario(7L);
        verify(sessionRevocationService).revocarSesionesUsuario(9L);
    }

    /**
     * El panel preselecciona el primer rol (ADMIN), así que sin esta exclusión
     * el administrador se cerraba la sesión a sí mismo al guardar: 401 en la
     * siguiente petición, refresh fallido y la UI diciendo que no tenía
     * permisos.
     */
    @Test
    void syncPermissions_NoRevocaLaSesionDelAdministradorQueHaceElCambio() {
        when(rolRepository.sincronizarPermisos(eq("ADMIN"), any(String[].class))).thenReturn(1);
        when(usuarioRolRepository.findIdsUsuarioByNombreRol("ADMIN")).thenReturn(List.of(1L, 42L));

        autenticarComoUsuario(1L);
        try {
            service.syncPermissions("ADMIN", List.of("USUARIO_VER"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(sessionRevocationService, never()).revocarSesionesUsuario(1L);
        verify(sessionRevocationService).revocarSesionesUsuario(42L);
    }

    private static void autenticarComoUsuario(Long idUsuario) {
        CustomUserDetails detalles = new CustomUserDetails(
                idUsuario, "admin@artisync.com", "x", true, true, true, true, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(detalles, null, List.of()));
    }

    @Test
    void syncPermissions_NoRevocaNada_CuandoElRolNoTieneUsuarios() {
        when(rolRepository.sincronizarPermisos(eq("SUPERVISOR"), any(String[].class))).thenReturn(1);
        when(usuarioRolRepository.findIdsUsuarioByNombreRol("SUPERVISOR")).thenReturn(List.of());

        service.syncPermissions("SUPERVISOR", List.of("USUARIO_VER"));

        verifyNoInteractions(sessionRevocationService);
    }

    @Test
    void syncPermissions_ThrowsBadRequest_WhenPermisoInexistente() {
        when(rolRepository.sincronizarPermisos(eq("SUPERVISOR"), any(String[].class)))
                .thenThrow(excepcionSql("23503", "Uno o mas permisos son inexistentes para el rol SUPERVISOR"));

        assertThrows(ResponseStatusException.class, () -> service.syncPermissions("SUPERVISOR", List.of("fantasma")));
    }

    @Test
    void syncPermissions_ThrowsNotFound_WhenRolNoExiste() {
        when(rolRepository.sincronizarPermisos(eq("FANTASMA"), any(String[].class)))
                .thenThrow(excepcionSql("P0002", "Rol no encontrado: FANTASMA"));

        assertThrows(ResponseStatusException.class, () -> service.syncPermissions("fantasma", List.of()));
    }

    @Test
    void createRole_AsignaPermisosIniciales() {
        Permiso permiso = Permiso.builder().idPermiso(1L).nombrePermiso("CATALOGO_VER").build();
        CreateRoleRequest req = new CreateRoleRequest("SUPERVISOR", "desc", List.of("catalogo_ver"));
        rolCustom.setPermisos(new HashSet<>(Set.of(permiso)));

        when(rolRepository.crearRol("SUPERVISOR", "desc", new String[]{"CATALOGO_VER"})).thenReturn(10L);
        when(rolRepository.findById(10L)).thenReturn(Optional.of(rolCustom));

        RolResponse res = service.createRole(req);

        assertEquals(List.of("CATALOGO_VER"), res.getPermisos());
    }

    @Test
    void createRole_ThrowsBadRequest_WhenPermisoInicialInexistente() {
        // fn_sincronizar_permisos_rol (invocada dentro de fn_crear_rol) lanza
        // ERRCODE 23503 cuando un codigo de permiso es invalido.
        CreateRoleRequest req = new CreateRoleRequest("SUPERVISOR", "desc", List.of("fantasma"));
        when(rolRepository.crearRol("SUPERVISOR", "desc", new String[]{"FANTASMA"}))
                .thenThrow(excepcionSql("23503", "Uno o mas permisos son inexistentes para el rol SUPERVISOR"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createRole(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
