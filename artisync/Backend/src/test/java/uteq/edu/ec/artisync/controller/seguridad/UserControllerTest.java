package uteq.edu.ec.artisync.controller.seguridad;
import uteq.edu.ec.artisync.controller.seguridad.*;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateUserRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.service.seguridad.UserService;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private Principal principal;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        when(principal.getName()).thenReturn("test@example.com");
    }

    @Test
    void getCurrentUser_ShouldReturnOk() {
        UserResponse userResponse = UserResponse.builder().correo("test@example.com").build();
        when(userService.getCurrentUser("test@example.com")).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = userController.getCurrentUser(principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("test@example.com", result.getBody().getCorreo());
    }

    @Test
    void updateCurrentUser_ShouldReturnOk() {
        UpdateUserRequest request = new UpdateUserRequest();
        UserResponse userResponse = UserResponse.builder().nombres("Nuevo").build();
        when(userService.updateCurrentUser("test@example.com", request)).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = userController.updateCurrentUser(principal, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Nuevo", result.getBody().getNombres());
    }

    @Test
    void deleteOwnAccount_ShouldReturnOk() {
        when(userService.deleteOwnAccount("test@example.com")).thenReturn(new RespuestaMensaje("Eliminado"));

        ResponseEntity<RespuestaMensaje> result = userController.deleteOwnAccount(principal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Eliminado", result.getBody().getMensaje());
    }
}

