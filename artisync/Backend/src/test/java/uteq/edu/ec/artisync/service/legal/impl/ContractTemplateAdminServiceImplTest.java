package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.legal.UpdateContractTemplateRequest;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateContractTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateResponse;
import uteq.edu.ec.artisync.entity.pedido.ContractTemplate;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.pedido.ContractTemplateRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pruebas unitarias de {@link ContractTemplateAdminServiceImpl}.
 * REQ-F-017 ampliado: catálogo de plantillas curado por ADMIN, con una única
 * plantilla predeterminada a la vez.
 */
@ExtendWith(MockitoExtension.class)
class ContractTemplateAdminServiceImplTest {

    @Mock private ContractTemplateRepository plantillaContratoRepository;

    @InjectMocks
    private ContractTemplateAdminServiceImpl servicio;

    private ContractTemplate predeterminadaActual;

    @BeforeEach
    void setUp() {
        predeterminadaActual = ContractTemplate.builder().idPlantilla(1L).versionLegal("v1.0")
                .nombrePlantilla("General (predeterminada)").cuerpoHtmlPlantilla("<html></html>")
                .esPredeterminada(true).activa(true).build();
    }

    @Test
    @DisplayName("create — nueva plantilla marcada como predeterminada desmarca la anterior")
    void crear_predeterminada_desmarcaAnterior() {
        CreateContractTemplateRequest peticion = CreateContractTemplateRequest.builder()
                .nombrePlantilla("Diseño gráfico").versionLegal("v-diseno")
                .cuerpoHtmlPlantilla("<html>{{nombre_cliente}}</html>")
                .esPredeterminada(true)
                .build();

        given(plantillaContratoRepository.findByVersionLegal("v-diseno")).willReturn(Optional.empty());
        given(plantillaContratoRepository.findByEsPredeterminadaTrue()).willReturn(Optional.of(predeterminadaActual));
        given(plantillaContratoRepository.save(any(ContractTemplate.class))).willAnswer(inv -> inv.getArgument(0));

        ContractTemplateResponse respuesta = servicio.create(peticion);

        assertThat(respuesta.getEsPredeterminada()).isTrue();
        assertThat(predeterminadaActual.getEsPredeterminada()).isFalse();
        verify(plantillaContratoRepository).flush();
    }

    @Test
    @DisplayName("create — rechaza version_legal duplicada")
    void crear_versionDuplicada_rechaza() {
        CreateContractTemplateRequest peticion = CreateContractTemplateRequest.builder()
                .nombrePlantilla("Otra").versionLegal("v1.0").cuerpoHtmlPlantilla("<html></html>").build();
        given(plantillaContratoRepository.findByVersionLegal("v1.0")).willReturn(Optional.of(predeterminadaActual));

        assertThatThrownBy(() -> servicio.create(peticion)).isInstanceOf(BusinessRuleException.class);
        verify(plantillaContratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("update — quitarle la condición de predeterminada sin asignar otra se rechaza")
    void editar_quitarPredeterminadaSinReemplazo_rechaza() {
        given(plantillaContratoRepository.findById(1L)).willReturn(Optional.of(predeterminadaActual));

        UpdateContractTemplateRequest peticion = UpdateContractTemplateRequest.builder()
                .nombrePlantilla("General").versionLegal("v1.0").cuerpoHtmlPlantilla("<html></html>")
                .esPredeterminada(false).activa(true)
                .build();

        assertThatThrownBy(() -> servicio.update(1L, peticion)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("deactivate — rechaza deactivate la plantilla predeterminada")
    void desactivar_predeterminada_rechaza() {
        given(plantillaContratoRepository.findById(1L)).willReturn(Optional.of(predeterminadaActual));

        assertThatThrownBy(() -> servicio.deactivate(1L)).isInstanceOf(BusinessRuleException.class);
        verify(plantillaContratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("deactivate — una plantilla no predeterminada se desactiva correctamente")
    void desactivar_noPredeterminada_ok() {
        ContractTemplate otra = ContractTemplate.builder().idPlantilla(2L).versionLegal("v2")
                .nombrePlantilla("Otra").cuerpoHtmlPlantilla("<html></html>")
                .esPredeterminada(false).activa(true).build();
        given(plantillaContratoRepository.findById(2L)).willReturn(Optional.of(otra));

        RespuestaMensaje respuesta = servicio.deactivate(2L);

        assertThat(respuesta.getMessage()).contains("desactivada");
        assertThat(otra.getActiva()).isFalse();
        verify(plantillaContratoRepository, times(1)).save(otra);
    }

    @Test
    @DisplayName("deactivate — plantilla inexistente lanza recurso no encontrado")
    void desactivar_inexistente_lanzaExcepcion() {
        given(plantillaContratoRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.deactivate(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listActive — solo devuelve las activas, en el DTO liviano del selector del creador")
    void listarActivas_devuelveResumen() {
        given(plantillaContratoRepository.findByActivaTrueOrderByNombrePlantillaAsc())
                .willReturn(List.of(predeterminadaActual));

        var resultado = servicio.listActive();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdPlantilla()).isEqualTo(1L);
        assertThat(resultado.get(0).getNombrePlantilla()).isEqualTo("General (predeterminada)");
        assertThat(resultado.get(0).isEsPropia()).isFalse();
    }

    @Test
    @DisplayName("listActiveVisibleTo (V45) — marca esPropia solo en las plantillas privadas del creador que consulta")
    void listarActivasVisiblesPara_marcaEsPropia() {
        ContractTemplate propia = ContractTemplate.builder().idPlantilla(5L).versionLegal("propia-7-abcd1234")
                .nombrePlantilla("Mi plantilla").cuerpoHtmlPlantilla("<html></html>")
                .esPredeterminada(false).activa(true).idCreador(7L).build();
        given(plantillaContratoRepository.findActivasVisiblesParaCreador(7L))
                .willReturn(List.of(predeterminadaActual, propia));

        var resultado = servicio.listActiveVisibleTo(7L);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).isEsPropia()).isFalse();
        assertThat(resultado.get(1).isEsPropia()).isTrue();
    }
}
