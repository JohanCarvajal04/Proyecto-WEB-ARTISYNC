package uteq.edu.ec.artisync.controller.seguridad;
import uteq.edu.ec.artisync.controller.seguridad.*;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.seguridad.request.CreateRoleRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateRoleRequest;
import uteq.edu.ec.artisync.dto.shared.MessageResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.RolResponse;
import uteq.edu.ec.artisync.service.seguridad.RolePermissionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolePermissionControllerTest {

    @Mock
    private RolePermissionService service;

    @InjectMocks
    private RolePermissionController controller;

    @Test
    void getAllRoles_Success() {
        when(service.getAllRoles()).thenReturn(List.of(
                RolResponse.builder().idRol(1L).nombreRol("ADMIN").build()
        ));

        ResponseEntity<List<RolResponse>> response = controller.getAllRoles();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("ADMIN", response.getBody().get(0).getNombreRol());
    }

    @Test
    void createRole_Success() {
        CreateRoleRequest req = new CreateRoleRequest("GESTOR", "Gestor general", List.of());
        when(service.createRole(any())).thenReturn(
                RolResponse.builder().idRol(5L).nombreRol("GESTOR").descripcionRol("Gestor general").build()
        );

        ResponseEntity<RolResponse> response = controller.createRole(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("GESTOR", response.getBody().getNombreRol());
    }

    @Test
    void updateRole_Success() {
        UpdateRoleRequest req = new UpdateRoleRequest("Nueva desc");
        when(service.updateRole(eq(5L), any())).thenReturn(
                RolResponse.builder().idRol(5L).nombreRol("GESTOR").descripcionRol("Nueva desc").build()
        );

        ResponseEntity<RolResponse> response = controller.updateRole(5L, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Nueva desc", response.getBody().getDescripcionRol());
    }

    @Test
    void deleteRole_Success() {
        ResponseEntity<MessageResponse> response = controller.deleteRole(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Rol eliminado exitosamente", response.getBody().getMensaje());
        verify(service).deleteRole(5L);
    }
}
