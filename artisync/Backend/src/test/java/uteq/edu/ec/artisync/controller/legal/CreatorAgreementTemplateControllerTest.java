package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.legal.UpdateOwnAgreementTemplateRequest;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateOwnAgreementTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.ICreatorAgreementTemplateService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatorAgreementTemplateControllerTest {

    @Mock
    private ICreatorAgreementTemplateService plantillaAcuerdoCreadorServicio;

    @InjectMocks
    private CreatorAgreementTemplateController controlador;

    private CustomUserDetails creador(Long idUsuario) {
        return new CustomUserDetails(idUsuario, "creador", "x", true, true, true, true, List.of());
    }

    @Test
    void crear_DebeRetornarCreadaParaElCreadorAutenticado() {
        CustomUserDetails usuario = creador(1L);
        CreateOwnAgreementTemplateRequest peticion = CreateOwnAgreementTemplateRequest.builder()
                .nombrePlantilla("Mi plantilla").cuerpoHtmlPlantilla("<p>...</p>").build();
        when(plantillaAcuerdoCreadorServicio.crear(1L, peticion))
                .thenReturn(ContractTemplateResponse.builder().idPlantilla(2L).idCreador(1L).build());

        ResponseEntity<ContractTemplateResponse> result = controlador.crear(peticion, usuario);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getIdCreador()).isEqualTo(1L);
    }

    @Test
    void editar_DebeRetornarLaPlantillaActualizada() {
        CustomUserDetails usuario = creador(1L);
        UpdateOwnAgreementTemplateRequest peticion = UpdateOwnAgreementTemplateRequest.builder()
                .nombrePlantilla("Actualizada").cuerpoHtmlPlantilla("<p>nueva</p>").activa(true).build();
        when(plantillaAcuerdoCreadorServicio.editar(1L, 2L, peticion))
                .thenReturn(ContractTemplateResponse.builder().idPlantilla(2L).nombrePlantilla("Actualizada").build());

        ResponseEntity<ContractTemplateResponse> result = controlador.editar(2L, peticion, usuario);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getNombrePlantilla()).isEqualTo("Actualizada");
    }

    @Test
    void listarPropias_DebeRetornarLasPlantillasDelCreadorAutenticado() {
        CustomUserDetails usuario = creador(1L);
        when(plantillaAcuerdoCreadorServicio.listarPropias(1L))
                .thenReturn(List.of(ContractTemplateResponse.builder().idPlantilla(2L).idCreador(1L).build()));

        ResponseEntity<List<ContractTemplateResponse>> result = controlador.listarPropias(usuario);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void desactivar_DebeRetornarMensajeDeConfirmacion() {
        CustomUserDetails usuario = creador(1L);
        when(plantillaAcuerdoCreadorServicio.desactivar(1L, 2L))
                .thenReturn(new RespuestaMensaje("Plantilla desactivada"));

        ResponseEntity<RespuestaMensaje> result = controlador.desactivar(2L, usuario);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getMensaje()).isEqualTo("Plantilla desactivada");
    }
}
