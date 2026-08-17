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
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRolRepository;

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

    @Test
    void deleteRole_Success() {
        when(rolRepository.findById(10L)).thenReturn(Optional.of(rolCustom));
        when(usuarioRolRepository.existsByRolIdRol(10L)).thenReturn(false);

        assertDoesNotThrow(() -> service.deleteRole(10L));
        verify(rolRepository).delete(rolCustom);
    }

    @Test
    void deleteRole_FailsForSystemRole() {
        Rol adminRol = Rol.builder().idRol(1L).nombreRol("ADMIN").build();
        when(rolRepository.findById(1L)).thenReturn(Optional.of(adminRol));

        assertThrows(ResponseStatusException.class, () -> service.deleteRole(1L));
        verify(rolRepository, never()).delete(any(Rol.class));
    }

    @Test
    void deleteRole_FailsWhenUsuariosAsignados() {
        when(rolRepository.findById(10L)).thenReturn(Optional.of(rolCustom));
        when(usuarioRolRepository.existsByRolIdRol(10L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.deleteRole(10L));
        verify(rolRepository, never()).delete(any(Rol.class));
    }

    @Test
    void deleteRole_ThrowsNotFound_WhenRolNoExiste() {
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.deleteRole(99L));
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

    @Test
    void syncPermissions_AsignaLosPermisosIndicados() {
        Permiso permiso = Permiso.builder().idPermiso(1L).nombrePermiso("CATALOGO_VER").build();
        when(rolRepository.findByNombreRol("SUPERVISOR")).thenReturn(Optional.of(rolCustom));
        when(permisoRepository.findByNombrePermiso("CATALOGO_VER")).thenReturn(Optional.of(permiso));
        when(rolRepository.save(any(Rol.class))).thenReturn(rolCustom);

        service.syncPermissions("supervisor", List.of("catalogo_ver"));

        assertEquals(Set.of(permiso), rolCustom.getPermisos());
    }

    @Test
    void syncPermissions_LimpiaPermisos_CuandoListaVacia() {
        when(rolRepository.findByNombreRol("SUPERVISOR")).thenReturn(Optional.of(rolCustom));
        when(rolRepository.save(any(Rol.class))).thenReturn(rolCustom);

        service.syncPermissions("SUPERVISOR", List.of());

        assertTrue(rolCustom.getPermisos().isEmpty());
    }

    @Test
    void syncPermissions_ThrowsBadRequest_WhenPermisoInexistente() {
        when(rolRepository.findByNombreRol("SUPERVISOR")).thenReturn(Optional.of(rolCustom));
        when(permisoRepository.findByNombrePermiso("FANTASMA")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.syncPermissions("SUPERVISOR", List.of("fantasma")));
    }

    @Test
    void syncPermissions_ThrowsNotFound_WhenRolNoExiste() {
        when(rolRepository.findByNombreRol("FANTASMA")).thenReturn(Optional.empty());

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
