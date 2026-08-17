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
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.seguridad.request.CreateRoleRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateRoleRequest;
import uteq.edu.ec.artisync.dto.seguridad.response.PermisoResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.RolResponse;
import uteq.edu.ec.artisync.entity.seguridad.Permiso;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.repository.seguridad.PermisoRepository;
import uteq.edu.ec.artisync.repository.seguridad.RolRepository;

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

    @Test
    void createRole_Success() {
        CreateRoleRequest req = new CreateRoleRequest("SUPERVISOR", "Rol de supervisión", List.of());
        when(rolRepository.findByNombreRol("SUPERVISOR")).thenReturn(Optional.empty());
        when(rolRepository.save(any(Rol.class))).thenReturn(rolCustom);

        RolResponse res = service.createRole(req);

        assertNotNull(res);
        assertEquals("SUPERVISOR", res.getNombreRol());
        verify(rolRepository).save(any(Rol.class));
    }

    @Test
    void createRole_ConflictWhenRoleExists() {
        CreateRoleRequest req = new CreateRoleRequest("SUPERVISOR", "Rol de supervisión", List.of());
        when(rolRepository.findByNombreRol("SUPERVISOR")).thenReturn(Optional.of(rolCustom));

        assertThrows(ResponseStatusException.class, () -> service.createRole(req));
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

        when(rolRepository.findByNombreRol("SUPERVISOR")).thenReturn(Optional.empty());
        when(permisoRepository.findByNombrePermiso("CATALOGO_VER")).thenReturn(Optional.of(permiso));
        when(rolRepository.save(any(Rol.class))).thenReturn(rolCustom);

        RolResponse res = service.createRole(req);

        assertEquals(List.of("CATALOGO_VER"), res.getPermisos());
    }

    @Test
    void createRole_ThrowsBadRequest_WhenPermisoInicialInexistente() {
        CreateRoleRequest req = new CreateRoleRequest("SUPERVISOR", "desc", List.of("fantasma"));
        when(rolRepository.findByNombreRol("SUPERVISOR")).thenReturn(Optional.empty());
        when(permisoRepository.findByNombrePermiso("FANTASMA")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.createRole(req));
    }
}
