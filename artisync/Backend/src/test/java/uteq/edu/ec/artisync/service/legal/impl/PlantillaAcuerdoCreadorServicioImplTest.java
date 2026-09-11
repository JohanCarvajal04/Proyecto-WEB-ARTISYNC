package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionActualizarPlantillaAcuerdoPropia;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionCrearPlantillaAcuerdoPropia;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPlantillaContrato;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.pedido.PlantillaContratoRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pruebas unitarias de {@link PlantillaAcuerdoCreadorServicioImpl} (V45): el
 * creador crea/edita/desactiva sus propias plantillas de acuerdo, aparte del
 * catálogo general de {@link PlantillaContratoAdminServicioImpl}, y nunca
 * puede tocar la de otro creador.
 */
@ExtendWith(MockitoExtension.class)
class PlantillaAcuerdoCreadorServicioImplTest {

    @Mock private PlantillaContratoRepository plantillaContratoRepository;

    @InjectMocks
    private PlantillaAcuerdoCreadorServicioImpl servicio;

    private static final Long ID_CREADOR = 7L;

    private PlantillaContrato plantillaPropia;

    @BeforeEach
    void setUp() {
        plantillaPropia = PlantillaContrato.builder().idPlantilla(10L).versionLegal("propia-7-abcd1234")
                .nombrePlantilla("Diseño de logo").cuerpoHtmlPlantilla("<html>{{nombre_cliente}}</html>")
                .esPredeterminada(false).activa(true).idCreador(ID_CREADOR).build();
    }

    @Test
    @DisplayName("crear — la plantilla queda marcada con el id del creador, nunca como predeterminada")
    void crear_quedaMarcadaConIdCreador() {
        PeticionCrearPlantillaAcuerdoPropia peticion = PeticionCrearPlantillaAcuerdoPropia.builder()
                .nombrePlantilla("Diseño de logo")
                .cuerpoHtmlPlantilla("<html>{{nombre_cliente}}</html>")
                .build();
        given(plantillaContratoRepository.save(any(PlantillaContrato.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaPlantillaContrato respuesta = servicio.crear(ID_CREADOR, peticion);

        assertThat(respuesta.getIdCreador()).isEqualTo(ID_CREADOR);
        assertThat(respuesta.getEsPredeterminada()).isFalse();
        assertThat(respuesta.getActiva()).isTrue();
        assertThat(respuesta.getVersionLegal()).startsWith("propia-" + ID_CREADOR + "-");
    }

    @Test
    @DisplayName("editar — actualiza nombre, cuerpo y estado de una plantilla propia")
    void editar_propia_actualiza() {
        given(plantillaContratoRepository.findByIdPlantillaAndIdCreador(10L, ID_CREADOR))
                .willReturn(Optional.of(plantillaPropia));
        given(plantillaContratoRepository.save(any(PlantillaContrato.class))).willAnswer(inv -> inv.getArgument(0));

        PeticionActualizarPlantillaAcuerdoPropia peticion = PeticionActualizarPlantillaAcuerdoPropia.builder()
                .nombrePlantilla("Diseño de logo v2")
                .cuerpoHtmlPlantilla("<html>nuevo</html>")
                .activa(false)
                .build();

        RespuestaPlantillaContrato respuesta = servicio.editar(ID_CREADOR, 10L, peticion);

        assertThat(respuesta.getNombrePlantilla()).isEqualTo("Diseño de logo v2");
        assertThat(respuesta.getActiva()).isFalse();
    }

    @Test
    @DisplayName("editar — una plantilla de OTRO creador no se encuentra (no se revela su existencia)")
    void editar_deOtroCreador_lanzaNoEncontrado() {
        given(plantillaContratoRepository.findByIdPlantillaAndIdCreador(10L, 999L))
                .willReturn(Optional.empty());

        PeticionActualizarPlantillaAcuerdoPropia peticion = PeticionActualizarPlantillaAcuerdoPropia.builder()
                .nombrePlantilla("x").cuerpoHtmlPlantilla("<html></html>").activa(true).build();

        assertThatThrownBy(() -> servicio.editar(999L, 10L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(plantillaContratoRepository, never()).save(any());
    }

    @Test
    @DisplayName("listarPropias — solo las plantillas de ese creador")
    void listarPropias_filtraPorCreador() {
        given(plantillaContratoRepository.findByIdCreadorOrderByNombrePlantillaAsc(ID_CREADOR))
                .willReturn(List.of(plantillaPropia));

        List<RespuestaPlantillaContrato> resultado = servicio.listarPropias(ID_CREADOR);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getIdCreador()).isEqualTo(ID_CREADOR);
    }

    @Test
    @DisplayName("desactivar — marca la plantilla propia como inactiva")
    void desactivar_propia_ok() {
        given(plantillaContratoRepository.findByIdPlantillaAndIdCreador(10L, ID_CREADOR))
                .willReturn(Optional.of(plantillaPropia));

        RespuestaMensaje respuesta = servicio.desactivar(ID_CREADOR, 10L);

        assertThat(respuesta.getMessage()).contains("desactivada");
        assertThat(plantillaPropia.getActiva()).isFalse();
        verify(plantillaContratoRepository).save(plantillaPropia);
    }

    @Test
    @DisplayName("desactivar — una plantilla que no le pertenece lanza recurso no encontrado")
    void desactivar_deOtroCreador_lanzaNoEncontrado() {
        given(plantillaContratoRepository.findByIdPlantillaAndIdCreador(10L, 999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.desactivar(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(plantillaContratoRepository, never()).save(any());
    }
}
