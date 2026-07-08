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
import uteq.edu.ec.artisync.dto.seguridad.response.RolResponse;
import uteq.edu.ec.artisync.entity.seguridad.Permiso;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.repository.seguridad.PermisoRepository;
import uteq.edu.ec.artisync.repository.seguridad.RolRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRolRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

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
}
