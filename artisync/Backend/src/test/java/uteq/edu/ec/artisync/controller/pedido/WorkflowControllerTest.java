package uteq.edu.ec.artisync.controller.pedido;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateWorkflowRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.StageConfigRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.SwapStagesRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.pedido.WorkflowResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.pedido.IWorkflowService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerTest {

    @Mock
    private IWorkflowService flujoTrabajoServicio;

    @InjectMocks
    private WorkflowController controlador;

    private CustomUserDetails mockUserDetails(boolean moderadorOAdmin) {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        if (moderadorOAdmin) {
            org.mockito.Mockito.lenient().when(user.getAuthorities()).thenAnswer(invocation -> List.of(new SimpleGrantedAuthority("FLUJO_MODERAR")));
        } else {
            org.mockito.Mockito.lenient().when(user.getAuthorities()).thenAnswer(invocation -> Collections.emptyList());
        }
        return user;
    }

    @Test
    void crearFlujo_devuelveCreated() {
        CustomUserDetails user = mockUserDetails(false);
        CreateWorkflowRequest peticion = new CreateWorkflowRequest();
        WorkflowResponse respuesta = new WorkflowResponse();
        when(flujoTrabajoServicio.crearFlujoTrabajo(1L, peticion)).thenReturn(respuesta);

        ResponseEntity<WorkflowResponse> res = controlador.crearFlujo(peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarFlujos_conModerador_devuelveOk() {
        CustomUserDetails user = mockUserDetails(true);
        List<WorkflowResponse> lista = Collections.emptyList();
        when(flujoTrabajoServicio.listarFlujosTrabajo(1L, true)).thenReturn(lista);

        ResponseEntity<List<WorkflowResponse>> res = controlador.listarFlujos(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void obtenerFlujo_devuelveOk() {
        CustomUserDetails user = mockUserDetails(false);
        WorkflowResponse respuesta = new WorkflowResponse();
        when(flujoTrabajoServicio.obtenerFlujoPorId(10L, 1L, false)).thenReturn(respuesta);

        ResponseEntity<WorkflowResponse> res = controlador.obtenerFlujo(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarFlujo_devuelveOk() {
        CustomUserDetails user = mockUserDetails(false);
        CreateWorkflowRequest peticion = new CreateWorkflowRequest();
        WorkflowResponse respuesta = new WorkflowResponse();
        when(flujoTrabajoServicio.actualizarFlujoTrabajo(10L, 1L, false, peticion)).thenReturn(respuesta);

        ResponseEntity<WorkflowResponse> res = controlador.actualizarFlujo(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void agregarEtapa_devuelveCreated() {
        CustomUserDetails user = mockUserDetails(false);
        StageConfigRequest peticion = new StageConfigRequest();
        WorkflowResponse respuesta = new WorkflowResponse();
        when(flujoTrabajoServicio.agregarEtapa(10L, 1L, false, peticion)).thenReturn(respuesta);

        ResponseEntity<WorkflowResponse> res = controlador.agregarEtapa(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarEtapa_devuelveOk() {
        CustomUserDetails user = mockUserDetails(false);
        StageConfigRequest peticion = new StageConfigRequest();
        WorkflowResponse respuesta = new WorkflowResponse();
        when(flujoTrabajoServicio.actualizarEtapa(10L, 20L, 1L, false, peticion)).thenReturn(respuesta);

        ResponseEntity<WorkflowResponse> res = controlador.actualizarEtapa(10L, 20L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void intercambiarOrdenEtapas_devuelveOk() {
        CustomUserDetails user = mockUserDetails(false);
        SwapStagesRequest peticion = new SwapStagesRequest();
        WorkflowResponse respuesta = new WorkflowResponse();
        when(flujoTrabajoServicio.intercambiarOrdenEtapas(10L, 1L, false, peticion)).thenReturn(respuesta);

        ResponseEntity<WorkflowResponse> res = controlador.intercambiarOrdenEtapas(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarEtapa_devuelveOk() {
        CustomUserDetails user = mockUserDetails(false);

        ResponseEntity<RespuestaMensaje> res = controlador.eliminarEtapa(10L, 20L, user);
        verify(flujoTrabajoServicio).eliminarEtapa(10L, 20L, 1L, false);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getMensaje()).contains("eliminada exitosamente");
    }
}
