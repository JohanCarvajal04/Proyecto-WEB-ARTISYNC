package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.response.legal.ContractTemplateSummaryResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IContractTemplateAdminService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ContractTemplateControllerTest {

    @Mock private IContractTemplateAdminService plantillaContratoAdminServicio;

    @InjectMocks
    private ContractTemplateController controller;

    @Test
    void listActive_devuelveLasPlantillasVisiblesParaElUsuario() {
        CustomUserDetails usuario = new CustomUserDetails(1L, "creador@test.dev", "x", true, true, true, true, List.of());
        List<ContractTemplateSummaryResponse> listado =
                List.of(ContractTemplateSummaryResponse.builder().idPlantilla(1L).build());
        given(plantillaContratoAdminServicio.listActiveVisibleTo(1L)).willReturn(listado);

        ResponseEntity<List<ContractTemplateSummaryResponse>> respuesta = controller.listActive(usuario);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(listado);
    }
}
