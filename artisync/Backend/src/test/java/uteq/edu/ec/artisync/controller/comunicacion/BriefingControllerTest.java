package uteq.edu.ec.artisync.controller.comunicacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.CreateBriefingTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.BriefingResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.BriefingService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BriefingControllerTest {

    @Mock
    private BriefingService briefingService;

    @InjectMocks
    private BriefingController controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void crearPlantilla_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        CreateBriefingTemplateRequest peticion = new CreateBriefingTemplateRequest();
        BriefingResponse respuesta = new BriefingResponse();
        when(briefingService.createTemplate(1L, peticion)).thenReturn(respuesta);

        ResponseEntity<BriefingResponse> res = controlador.createTemplate(peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerMisPlantillas_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<BriefingResponse> lista = Collections.emptyList();
        when(briefingService.getMyTemplates(1L)).thenReturn(lista);

        ResponseEntity<List<BriefingResponse>> res = controlador.getMyTemplates(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void editarPlantilla_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        CreateBriefingTemplateRequest peticion = new CreateBriefingTemplateRequest();
        BriefingResponse respuesta = new BriefingResponse();
        when(briefingService.updateTemplate(10L, 1L, peticion)).thenReturn(respuesta);

        ResponseEntity<BriefingResponse> res = controlador.updateTemplate(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarPlantilla_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaMensaje respuesta = new RespuestaMensaje("Ok");
        when(briefingService.deleteTemplate(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaMensaje> res = controlador.deleteTemplate(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerBriefing_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        BriefingResponse respuesta = new BriefingResponse();
        when(briefingService.getBriefing(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<BriefingResponse> res = controlador.getBriefing(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }
}
