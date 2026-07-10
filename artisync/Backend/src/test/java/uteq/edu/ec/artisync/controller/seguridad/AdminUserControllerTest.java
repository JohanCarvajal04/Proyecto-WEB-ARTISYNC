package uteq.edu.ec.artisync.controller.seguridad;
import uteq.edu.ec.artisync.controller.seguridad.*;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.seguridad.request.ChangeEstadoRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.service.seguridad.AdminUserService;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void getAllUsers_ShouldReturnOk() {
        PagedResponse<UserResponse> pagedResponse = new PagedResponse<>(List.of(), 0, 10, 0, 0, true);
        when(adminUserService.getAllUsers(any(Pageable.class))).thenReturn(pagedResponse);

        ResponseEntity<PagedResponse<UserResponse>> result = adminUserController.getAllUsers(0, 10, "idUsuario", "asc");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(0, result.getBody().getContent().size());
    }

    @Test
    void getUserById_ShouldReturnOk() {
        UserResponse userResponse = UserResponse.builder().idUsuario(1L).build();
        when(adminUserService.getUserById(1L)).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = adminUserController.getUserById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1L, result.getBody().getIdUsuario());
    }

    @Test
    void changeEstado_ShouldReturnOk() {
        ChangeEstadoRequest request = new ChangeEstadoRequest();
        UserResponse userResponse = UserResponse.builder().estadoCuenta(false).build();
        when(adminUserService.changeEstado(eq(1L), any(ChangeEstadoRequest.class))).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = adminUserController.changeEstado(1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(false, result.getBody().getEstadoCuenta());
    }

    @Test
    void deleteUser_ShouldReturnNoContent() {
        org.mockito.Mockito.doNothing().when(adminUserService).deleteUser(1L);

        ResponseEntity<Void> result = adminUserController.deleteUser(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}

