package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.legal.UpdateOwnAgreementTemplateRequest;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateOwnAgreementTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateResponse;
import uteq.edu.ec.artisync.entity.pedido.ContractTemplate;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.pedido.ContractTemplateRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pruebas unitarias de {@link CreatorAgreementTemplateServiceImpl} (V45): el
 * creador crea/edita/desactiva sus propias plantillas de acuerdo, aparte del
 * catálogo general de {@link ContractTemplateAdminServiceImpl}, y nunca
 * puede tocar la de otro creador.
 */
@ExtendWith(MockitoExtension.class)
class CreatorAgreementTemplateServiceImplTest {

    @Mock private ContractTemplateRepository plantillaContratoRepository;

    @InjectMocks
    private CreatorAgreementTemplateServiceImpl servicio;

    private static final Long ID_CREADOR = 7L;

    private ContractTemplate plantillaPropia;

    @BeforeEach
    void setUp() {
        plantillaPropia = ContractTemplate.builder().idPlantilla(10L).versionLegal("propia-7-abcd1234")
                .nombrePlantilla("Diseño de logo").cuerpoHtmlPlantilla("<html>{{nombre_cliente}}</html>")
                .esPredeterminada(false).activa(true).idCreador(ID_CREADOR).build();
    }

    @Test
    @DisplayName("create — la plantilla queda marcada con el id del creador, nunca como predeterminada")
    void crear_quedaMarcadaConIdCreador() {
        CreateOwnAgreementTemplateRequest peticion = CreateOwnAgreementTemplateRequest.builder()
                .nombrePlantilla("Diseño de logo")
                .cuerpoHtmlPlantilla("<html>{{nombre_cliente}}</html>")
                .build();
        given(plantillaContratoRepository.save(any(ContractTemplate.class))).willAnswer(inv -> inv.getArgument(0));

        ContractTemplateResponse respuesta = servicio.create(ID_CREADOR, peticion);

        assertThat(respuesta.getIdCreador()).isEqualTo(ID_CREADOR);
        assertThat(respuesta.getEsPredeterminada()).isFalse();
        assertThat(respuesta.getActiva()).isTrue();
        assertThat(respuesta.getVersionLegal()).startsWith("propia-" + ID_CREADOR + "-");
    }

    @Test
    @DisplayName("update — actualiza nombre, cuerpo y estado de una plantilla propia")
    void editar_propia_actualiza() {
        given(plantillaContratoRepository.findByIdPlantillaAndIdCreador(10L, ID_CREADOR))
                .willReturn(Optional.of(plantillaPropia));
        given(plantillaContratoRepository.save(any(ContractTemplate.class))).willAnswer(inv -> inv.getArgument(0));

        UpdateOwnAgreementTemplateRequest peticion = UpdateOwnAgreementTemplateRequest.builder()
                .nombrePlantilla("Diseño de logo v2")
                .cuerpoHtmlPlantilla("<html>nuevo</html>")
                .activa(false)
                .build();

        ContractTemplateResponse respuesta = servicio.update(ID_CREADOR, 10L, peticion);

        assertThat(respuesta.getNombrePlantilla()).isEqualTo("Diseño de logo v2");
        assertThat(respuesta.getActiva()).isFalse();
    }

    @Test
    @DisplayName("update — una plantilla de OTRO creador no se encuentra (no se revela su existencia)")
    void editar_deOtroCreador_lanzaNoEncontrado() {
        given(plantillaContratoRepository.findByIdPlantillaAndIdCreador(10L, 999L))
                .willReturn(Optional.empty());

        UpdateOwnAgreementTemplateRequest peticion = UpdateOwnAgreementTemplateRequest.builder()
                .nombrePlantilla("x").cuerpoHtmlPlantilla("<html></html>").activa(true).build();

        assertThatThrownBy(() -> servicio.update(999L, 10L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(plantillaContratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("listOwn — solo las plantillas de ese creador")
    void listarPropias_filtraPorCreador() {
        given(plantillaContratoRepository.findByIdCreadorOrderByNombrePlantillaAsc(ID_CREADOR))
                .willReturn(List.of(plantillaPropia));

        List<ContractTemplateResponse> resultado = servicio.listOwn(ID_CREADOR);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdCreador()).isEqualTo(ID_CREADOR);
    }

    @Test
    @DisplayName("deactivate — marca la plantilla propia como inactiva")
    void desactivar_propia_ok() {
        given(plantillaContratoRepository.findByIdPlantillaAndIdCreador(10L, ID_CREADOR))
                .willReturn(Optional.of(plantillaPropia));

        RespuestaMensaje respuesta = servicio.deactivate(ID_CREADOR, 10L);

        assertThat(respuesta.getMessage()).contains("desactivada");
        assertThat(plantillaPropia.getActiva()).isFalse();
        verify(plantillaContratoRepository).save(plantillaPropia);
    }

    @Test
    @DisplayName("deactivate — una plantilla que no le pertenece lanza recurso no encontrado")
    void desactivar_deOtroCreador_lanzaNoEncontrado() {
        given(plantillaContratoRepository.findByIdPlantillaAndIdCreador(10L, 999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.deactivate(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(plantillaContratoRepository, never()).save(any());
    }
}
