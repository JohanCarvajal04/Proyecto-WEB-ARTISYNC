package uteq.edu.ec.artisync.controller.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.request.ChangePasswordRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateUserRequest;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.service.seguridad.UserService;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AlmacenamientoDocumentos almacenamientoDocumentos;

    @Mock
    private Principal principal;

    @InjectMocks
    private UserController userController;

    @Test
    void getCurrentUser_devuelveOk() {
        when(principal.getName()).thenReturn("test@test.com");
        UserResponse response = new UserResponse();
        when(userService.getCurrentUser("test@test.com")).thenReturn(response);

        ResponseEntity<UserResponse> res = userController.getCurrentUser(principal);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(response);
    }

    @Test
    void updateCurrentUser_devuelveOk() {
        when(principal.getName()).thenReturn("test@test.com");
        UpdateUserRequest request = new UpdateUserRequest();
        UserResponse response = new UserResponse();
        when(userService.updateCurrentUser("test@test.com", request)).thenReturn(response);

        ResponseEntity<UserResponse> res = userController.updateCurrentUser(principal, request);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(response);
    }

    @Test
    void changePassword_devuelveOk() {
        when(principal.getName()).thenReturn("test@test.com");
        ChangePasswordRequest request = new ChangePasswordRequest();
        RespuestaMensaje response = new RespuestaMensaje("Ok");
        when(userService.changePassword("test@test.com", request)).thenReturn(response);

        ResponseEntity<RespuestaMensaje> res = userController.changePassword(principal, request);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(response);
    }

    @Test
    void deleteOwnAccount_devuelveOk() {
        when(principal.getName()).thenReturn("test@test.com");
        RespuestaMensaje response = new RespuestaMensaje("Ok");
        when(userService.deleteOwnAccount("test@test.com")).thenReturn(response);

        ResponseEntity<RespuestaMensaje> res = userController.deleteOwnAccount(principal);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(response);
    }

    @Test
    void revokeAllMySessions_devuelveOk() {
        when(principal.getName()).thenReturn("test@test.com");
        RespuestaMensaje response = new RespuestaMensaje("Ok");
        when(userService.revokeAllMySessions("test@test.com")).thenReturn(response);

        ResponseEntity<RespuestaMensaje> res = userController.revokeAllMySessions(principal);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(response);
    }

    @Test
    void uploadProfilePicture_devuelveOk() {
        when(principal.getName()).thenReturn("test@test.com");
        MultipartFile foto = mock(MultipartFile.class);
        UserResponse response = new UserResponse();
        when(userService.uploadProfilePicture("test@test.com", foto)).thenReturn(response);

        ResponseEntity<UserResponse> res = userController.uploadProfilePicture(principal, foto);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(response);
    }

    @Test
    void servirFotoPerfil_pathInvalido_lanzaExcepcion() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/usuarios/foto/privado/archivo.jpg");

        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> userController.servirFotoPerfil(request));
    }

    @Test
    void servirFotoPerfil_pathValido_devuelveOk() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/usuarios/foto/perfiles/uuid.jpg");
        when(almacenamientoDocumentos.leer("perfiles/uuid.jpg")).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> res = userController.servirFotoPerfil(request);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsExactly(1, 2, 3);
        assertThat(res.getHeaders().getContentType().toString()).isEqualTo("image/jpeg");
    }
}
