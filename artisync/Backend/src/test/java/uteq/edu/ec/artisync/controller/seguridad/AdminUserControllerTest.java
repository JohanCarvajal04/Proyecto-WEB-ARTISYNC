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
import uteq.edu.ec.artisync.dto.peticion.seguridad.UserFilter;
import uteq.edu.ec.artisync.dto.seguridad.request.ChangeEstadoRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.seguridad.AdminUserService;
import uteq.edu.ec.artisync.service.seguridad.PrivacyService;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportChartType;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AdminUserControllerTest.exportar_/anonimizarUsuario_ son pruebas de
 * caracterización agregadas ANTES de renombrar export/anonymizeUser
 * (el controlador estaba en 40% de metodos cubiertos: manejan exportación
 * masiva de PII y el disparo de anonimización GDPR, las dos acciones de
 * mayor riesgo real del controlador según OBS-TR-01).
 */
@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private PrivacyService privacidadService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void getAllUsers_ShouldReturnOk() {
        PagedResponse<UserResponse> pagedResponse = new PagedResponse<>(List.of(), 0, 10, 0, 0, true);
        when(adminUserService.getAllUsers(any(UserFilter.class), any(Pageable.class))).thenReturn(pagedResponse);

        ResponseEntity<PagedResponse<UserResponse>> result =
                adminUserController.getAllUsers(new UserFilter(), 0, 10, "idUsuario", "asc");

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
        CustomUserDetails admin = new CustomUserDetails(99L, "admin@test.dev", "x", true, true, true, true, List.of());
        when(adminUserService.changeStatus(eq(1L), any(ChangeEstadoRequest.class), eq(99L))).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = adminUserController.changeStatus(1L, request, admin);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(false, result.getBody().getEstadoCuenta());
    }

    @Test
    void deleteUser_ShouldReturnNoContent() {
        CustomUserDetails admin = new CustomUserDetails(99L, "admin@test.dev", "x", true, true, true, true, List.of());
        org.mockito.Mockito.doNothing().when(adminUserService).deleteUser(1L, 99L);

        ResponseEntity<Void> result = adminUserController.deleteUser(1L, admin);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void exportar_ShouldReturnDocumentoAttachment() {
        CustomUserDetails admin = new CustomUserDetails(99L, "admin@test.dev", "x", true, true, true, true, List.of());
        GeneratedDocument documento = new GeneratedDocument(new byte[]{1, 2, 3}, "text/csv", "usuarios.csv");
        when(adminUserService.export(any(UserFilter.class), eq(ReportFormat.CSV), eq(ReportChartType.AMBAS), any(), any(), eq("admin@test.dev")))
                .thenReturn(documento);

        ResponseEntity<byte[]> result = adminUserController.export(
                new UserFilter(), ReportFormat.CSV, ReportChartType.AMBAS, null, null, adminAuthentication("admin@test.dev"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertArrayEquals(new byte[]{1, 2, 3}, result.getBody());
    }

    @Test
    void anonimizarUsuario_ShouldDelegateToPrivacyServiceAndReturnOk() {
        CustomUserDetails admin = new CustomUserDetails(99L, "admin@test.dev", "x", true, true, true, true, List.of());
        RespuestaMensaje mensaje = new RespuestaMensaje("Datos del usuario suprimidos.");
        when(privacidadService.anonymizeUserAsAdmin(1L, 99L)).thenReturn(mensaje);

        ResponseEntity<RespuestaMensaje> result = adminUserController.anonymizeUser(1L, admin);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(privacidadService).anonymizeUserAsAdmin(1L, 99L);
    }

    private org.springframework.security.core.Authentication adminAuthentication(String correo) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(correo, null, List.of());
    }
}

