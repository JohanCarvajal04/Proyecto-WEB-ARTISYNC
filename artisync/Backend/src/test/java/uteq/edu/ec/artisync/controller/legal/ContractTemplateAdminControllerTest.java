package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.request.legal.CreateContractTemplateRequest;
import uteq.edu.ec.artisync.dto.request.legal.UpdateContractTemplateRequest;
import uteq.edu.ec.artisync.dto.response.comun.MessageResponse;
import uteq.edu.ec.artisync.dto.response.legal.ContractTemplateResponse;
import uteq.edu.ec.artisync.service.legal.IContractTemplateAdminService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ContractTemplateAdminControllerTest {

    @Mock private IContractTemplateAdminService plantillaContratoAdminServicio;

    @InjectMocks
    private ContractTemplateAdminController controller;

    @Test
    void create_devuelve201ConLaPlantillaCreada() {
        CreateContractTemplateRequest peticion = CreateContractTemplateRequest.builder()
                .nombrePlantilla("Plantilla base").versionLegal("v1").cuerpoHtmlPlantilla("<p>Contrato</p>").build();
        ContractTemplateResponse creada = ContractTemplateResponse.builder().idPlantilla(1L).build();
        given(plantillaContratoAdminServicio.create(peticion)).willReturn(creada);

        ResponseEntity<ContractTemplateResponse> respuesta = controller.create(peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(creada);
    }

    @Test
    void update_devuelveLaPlantillaActualizada() {
        UpdateContractTemplateRequest peticion = UpdateContractTemplateRequest.builder()
                .nombrePlantilla("Plantilla base v2").versionLegal("v2").cuerpoHtmlPlantilla("<p>Contrato v2</p>").build();
        ContractTemplateResponse actualizada = ContractTemplateResponse.builder().idPlantilla(1L).build();
        given(plantillaContratoAdminServicio.update(1L, peticion)).willReturn(actualizada);

        ResponseEntity<ContractTemplateResponse> respuesta = controller.update(1L, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(actualizada);
    }

    @Test
    void listAll_devuelveElCatalogoCompleto() {
        List<ContractTemplateResponse> listado = List.of(ContractTemplateResponse.builder().idPlantilla(1L).build());
        given(plantillaContratoAdminServicio.listAll()).willReturn(listado);

        ResponseEntity<List<ContractTemplateResponse>> respuesta = controller.listAll();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(listado);
    }

    @Test
    void deactivate_devuelveElMensajeDeConfirmacion() {
        MessageResponse mensaje = new MessageResponse("Plantilla desactivada");
        given(plantillaContratoAdminServicio.deactivate(1L)).willReturn(mensaje);

        ResponseEntity<MessageResponse> respuesta = controller.deactivate(1L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(mensaje);
    }
}
