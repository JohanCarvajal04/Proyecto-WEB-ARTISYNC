package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionActualizarPlantillaContrato;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionCrearPlantillaContrato;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPlantillaContrato;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.pedido.PlantillaContratoRepository;

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
 * Pruebas unitarias de {@link PlantillaContratoAdminServicioImpl}.
 * REQ-F-017 ampliado: catálogo de plantillas curado por ADMIN, con una única
 * plantilla predeterminada a la vez.
 */
@ExtendWith(MockitoExtension.class)
class PlantillaContratoAdminServicioImplTest {

    @Mock private PlantillaContratoRepository plantillaContratoRepository;

    @InjectMocks
    private PlantillaContratoAdminServicioImpl servicio;

    private PlantillaContrato predeterminadaActual;

    @BeforeEach
    void setUp() {
        predeterminadaActual = PlantillaContrato.builder().idPlantilla(1L).versionLegal("v1.0")
                .nombrePlantilla("General (predeterminada)").cuerpoHtmlPlantilla("<html></html>")
                .esPredeterminada(true).activa(true).build();
    }

    @Test
    @DisplayName("crear — nueva plantilla marcada como predeterminada desmarca la anterior")
    void crear_predeterminada_desmarcaAnterior() {
        PeticionCrearPlantillaContrato peticion = PeticionCrearPlantillaContrato.builder()
                .nombrePlantilla("Diseño gráfico").versionLegal("v-diseno")
                .cuerpoHtmlPlantilla("<html>{{nombre_cliente}}</html>")
                .esPredeterminada(true)
                .build();

        given(plantillaContratoRepository.findByVersionLegal("v-diseno")).willReturn(Optional.empty());
        given(plantillaContratoRepository.findByEsPredeterminadaTrue()).willReturn(Optional.of(predeterminadaActual));
        given(plantillaContratoRepository.save(any(PlantillaContrato.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaPlantillaContrato respuesta = servicio.crear(peticion);

        assertThat(respuesta.getEsPredeterminada()).isTrue();
        assertThat(predeterminadaActual.getEsPredeterminada()).isFalse();
        verify(plantillaContratoRepository).flush();
    }

    @Test
    @DisplayName("crear — rechaza version_legal duplicada")
    void crear_versionDuplicada_rechaza() {
        PeticionCrearPlantillaContrato peticion = PeticionCrearPlantillaContrato.builder()
                .nombrePlantilla("Otra").versionLegal("v1.0").cuerpoHtmlPlantilla("<html></html>").build();
        given(plantillaContratoRepository.findByVersionLegal("v1.0")).willReturn(Optional.of(predeterminadaActual));

        assertThatThrownBy(() -> servicio.crear(peticion)).isInstanceOf(ExcepcionReglaNegocio.class);
        verify(plantillaContratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar — quitarle la condición de predeterminada sin asignar otra se rechaza")
    void editar_quitarPredeterminadaSinReemplazo_rechaza() {
        given(plantillaContratoRepository.findById(1L)).willReturn(Optional.of(predeterminadaActual));

        PeticionActualizarPlantillaContrato peticion = PeticionActualizarPlantillaContrato.builder()
                .nombrePlantilla("General").versionLegal("v1.0").cuerpoHtmlPlantilla("<html></html>")
                .esPredeterminada(false).activa(true)
                .build();

        assertThatThrownBy(() -> servicio.editar(1L, peticion)).isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("desactivar — rechaza desactivar la plantilla predeterminada")
    void desactivar_predeterminada_rechaza() {
        given(plantillaContratoRepository.findById(1L)).willReturn(Optional.of(predeterminadaActual));

        assertThatThrownBy(() -> servicio.desactivar(1L)).isInstanceOf(ExcepcionReglaNegocio.class);
        verify(plantillaContratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("desactivar — una plantilla no predeterminada se desactiva correctamente")
    void desactivar_noPredeterminada_ok() {
        PlantillaContrato otra = PlantillaContrato.builder().idPlantilla(2L).versionLegal("v2")
                .nombrePlantilla("Otra").cuerpoHtmlPlantilla("<html></html>")
                .esPredeterminada(false).activa(true).build();
        given(plantillaContratoRepository.findById(2L)).willReturn(Optional.of(otra));

        RespuestaMensaje respuesta = servicio.desactivar(2L);

        assertThat(respuesta.getMessage()).contains("desactivada");
        assertThat(otra.getActiva()).isFalse();
        verify(plantillaContratoRepository, times(1)).save(otra);
    }

    @Test
    @DisplayName("desactivar — plantilla inexistente lanza recurso no encontrado")
    void desactivar_inexistente_lanzaExcepcion() {
        given(plantillaContratoRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.desactivar(99L)).isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("listarActivas — solo devuelve las activas, en el DTO liviano del selector del creador")
    void listarActivas_devuelveResumen() {
        given(plantillaContratoRepository.findByActivaTrueOrderByNombrePlantillaAsc())
                .willReturn(List.of(predeterminadaActual));

        var resultado = servicio.listarActivas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdPlantilla()).isEqualTo(1L);
        assertThat(resultado.get(0).getNombrePlantilla()).isEqualTo("General (predeterminada)");
        assertThat(resultado.get(0).isEsPropia()).isFalse();
    }

    @Test
    @DisplayName("listarActivasVisiblesPara (V45) — marca esPropia solo en las plantillas privadas del creador que consulta")
    void listarActivasVisiblesPara_marcaEsPropia() {
        PlantillaContrato propia = PlantillaContrato.builder().idPlantilla(5L).versionLegal("propia-7-abcd1234")
                .nombrePlantilla("Mi plantilla").cuerpoHtmlPlantilla("<html></html>")
                .esPredeterminada(false).activa(true).idCreador(7L).build();
        given(plantillaContratoRepository.findActivasVisiblesParaCreador(7L))
                .willReturn(List.of(predeterminadaActual, propia));

        var resultado = servicio.listarActivasVisiblesPara(7L);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).isEsPropia()).isFalse();
        assertThat(resultado.get(1).isEsPropia()).isTrue();
    }
}
