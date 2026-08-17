package uteq.edu.ec.artisync.service.pedido.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearFlujoTrabajo;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionEtapaConfig;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaFlujoTrabajo;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;
import uteq.edu.ec.artisync.entity.pedido.EtapaFlujo;
import uteq.edu.ec.artisync.entity.pedido.FlujoEtapaConfig;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.FlujoTrabajoRepository;
import uteq.edu.ec.artisync.repository.pedido.EtapaFlujoRepository;
import uteq.edu.ec.artisync.repository.pedido.FlujoEtapaConfigRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlujoTrabajoServicioImplTest {

    @Mock private FlujoTrabajoRepository flujoTrabajoRepository;
    @Mock private EtapaFlujoRepository etapaFlujoRepository;
    @Mock private FlujoEtapaConfigRepository flujoEtapaConfigRepository;

    @InjectMocks
    private FlujoTrabajoServicioImpl flujoTrabajoServicio;

    private FlujoTrabajo flujo;

    @BeforeEach
    void setUp() {
        flujo = FlujoTrabajo.builder().idFlujo(1L).nombreFlujo("Flujo estandar").descripcionFlujo("desc").build();
    }

    @Test
    @DisplayName("crearFlujoTrabajo guarda el flujo sin etapas cuando no se proporcionan")
    void crearFlujoTrabajo_sinEtapas() {
        PeticionCrearFlujoTrabajo peticion = PeticionCrearFlujoTrabajo.builder().nombreFlujo("Flujo estandar").build();
        given(flujoTrabajoRepository.existsByNombreFlujo("Flujo estandar")).willReturn(false);
        given(flujoTrabajoRepository.save(any(FlujoTrabajo.class))).willReturn(flujo);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        RespuestaFlujoTrabajo respuesta = flujoTrabajoServicio.crearFlujoTrabajo(peticion);

        assertThat(respuesta.getNombreFlujo()).isEqualTo("Flujo estandar");
        verify(flujoEtapaConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearFlujoTrabajo rechaza un nombre duplicado")
    void crearFlujoTrabajo_rechazaDuplicado() {
        PeticionCrearFlujoTrabajo peticion = PeticionCrearFlujoTrabajo.builder().nombreFlujo("Flujo estandar").build();
        given(flujoTrabajoRepository.existsByNombreFlujo("Flujo estandar")).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.crearFlujoTrabajo(peticion))
                .isInstanceOf(ExcepcionRecursoDuplicado.class);
    }

    @Test
    @DisplayName("crearFlujoTrabajo crea las etapas indicadas reutilizando etapas existentes")
    void crearFlujoTrabajo_conEtapas() {
        EtapaFlujo etapa = EtapaFlujo.builder().idEtapa(1L).nombreEtapa("Revision").build();
        PeticionEtapaConfig etapaConfig = PeticionEtapaConfig.builder().nombreEtapa("Revision").numeroOrden(1).esEtapaFinal(false).build();
        PeticionCrearFlujoTrabajo peticion = PeticionCrearFlujoTrabajo.builder()
                .nombreFlujo("Flujo con etapas").etapas(List.of(etapaConfig)).build();

        given(flujoTrabajoRepository.existsByNombreFlujo("Flujo con etapas")).willReturn(false);
        given(flujoTrabajoRepository.save(any(FlujoTrabajo.class))).willReturn(flujo);
        given(etapaFlujoRepository.findByNombreEtapa("Revision")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.crearFlujoTrabajo(peticion);

        verify(flujoEtapaConfigRepository).save(any(FlujoEtapaConfig.class));
        verify(etapaFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearFlujoTrabajo crea una etapa nueva si no existe todavia")
    void crearFlujoTrabajo_creaEtapaNueva() {
        EtapaFlujo nueva = EtapaFlujo.builder().idEtapa(2L).nombreEtapa("Entrega").build();
        PeticionEtapaConfig etapaConfig = PeticionEtapaConfig.builder().nombreEtapa("Entrega").numeroOrden(1).esEtapaFinal(true).build();
        PeticionCrearFlujoTrabajo peticion = PeticionCrearFlujoTrabajo.builder()
                .nombreFlujo("Flujo con etapas").etapas(List.of(etapaConfig)).build();

        given(flujoTrabajoRepository.existsByNombreFlujo("Flujo con etapas")).willReturn(false);
        given(flujoTrabajoRepository.save(any(FlujoTrabajo.class))).willReturn(flujo);
        given(etapaFlujoRepository.findByNombreEtapa("Entrega")).willReturn(Optional.empty());
        given(etapaFlujoRepository.save(any(EtapaFlujo.class))).willReturn(nueva);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.crearFlujoTrabajo(peticion);

        verify(etapaFlujoRepository).save(any(EtapaFlujo.class));
    }

    @Test
    @DisplayName("listarFlujosTrabajo mapea todos los flujos")
    void listarFlujosTrabajo_mapea() {
        given(flujoTrabajoRepository.findAll()).willReturn(List.of(flujo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        assertThat(flujoTrabajoServicio.listarFlujosTrabajo()).hasSize(1);
    }

    @Test
    @DisplayName("obtenerFlujoPorId lanza recurso no encontrado si no existe")
    void obtenerFlujoPorId_inexistente() {
        given(flujoTrabajoRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> flujoTrabajoServicio.obtenerFlujoPorId(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("actualizarFlujoTrabajo cambia nombre y descripcion")
    void actualizarFlujoTrabajo_cambiaDatos() {
        PeticionCrearFlujoTrabajo peticion = PeticionCrearFlujoTrabajo.builder()
                .nombreFlujo("Renombrado").descripcionFlujo("nueva desc").build();
        given(flujoTrabajoRepository.findById(1L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        RespuestaFlujoTrabajo respuesta = flujoTrabajoServicio.actualizarFlujoTrabajo(1L, peticion);

        assertThat(respuesta.getNombreFlujo()).isEqualTo("Renombrado");
    }

    @Test
    @DisplayName("agregarEtapa rechaza una etapa duplicada en el flujo")
    void agregarEtapa_rechazaDuplicada() {
        EtapaFlujo etapa = EtapaFlujo.builder().idEtapa(1L).nombreEtapa("Revision").build();
        PeticionEtapaConfig peticion = PeticionEtapaConfig.builder().nombreEtapa("Revision").numeroOrden(1).build();

        given(flujoTrabajoRepository.findById(1L)).willReturn(Optional.of(flujo));
        given(etapaFlujoRepository.findByNombreEtapa("Revision")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.agregarEtapa(1L, peticion))
                .isInstanceOf(ExcepcionRecursoDuplicado.class);
    }

    @Test
    @DisplayName("agregarEtapa guarda la nueva configuracion cuando no esta duplicada")
    void agregarEtapa_guarda() {
        EtapaFlujo etapa = EtapaFlujo.builder().idEtapa(1L).nombreEtapa("Revision").build();
        PeticionEtapaConfig peticion = PeticionEtapaConfig.builder().nombreEtapa("Revision").numeroOrden(1).build();

        given(flujoTrabajoRepository.findById(1L)).willReturn(Optional.of(flujo));
        given(etapaFlujoRepository.findByNombreEtapa("Revision")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(1L, 1L)).willReturn(false);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        assertThat(flujoTrabajoServicio.agregarEtapa(1L, peticion)).isNotNull();
        verify(flujoEtapaConfigRepository).save(any(FlujoEtapaConfig.class));
    }

    @Test
    @DisplayName("actualizarEtapa cambia orden y marca final cuando pertenece al flujo")
    void actualizarEtapa_cambiaDatos() {
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(flujo).numeroOrden(1).esEtapaFinal(false).build();
        PeticionEtapaConfig peticion = PeticionEtapaConfig.builder().numeroOrden(2).esEtapaFinal(true).build();

        given(flujoTrabajoRepository.findById(1L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.actualizarEtapa(1L, 5L, peticion);

        assertThat(config.getNumeroOrden()).isEqualTo(2);
        assertThat(config.getEsEtapaFinal()).isTrue();
    }

    @Test
    @DisplayName("actualizarEtapa rechaza una etapa que no pertenece al flujo")
    void actualizarEtapa_rechazaOtroFlujo() {
        FlujoTrabajo otroFlujo = FlujoTrabajo.builder().idFlujo(2L).build();
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(otroFlujo).build();
        PeticionEtapaConfig peticion = PeticionEtapaConfig.builder().numeroOrden(2).build();

        given(flujoTrabajoRepository.findById(1L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));

        assertThatThrownBy(() -> flujoTrabajoServicio.actualizarEtapa(1L, 5L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("eliminarEtapa borra la configuracion cuando pertenece al flujo")
    void eliminarEtapa_borra() {
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(flujo).build();
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));

        flujoTrabajoServicio.eliminarEtapa(1L, 5L);

        verify(flujoEtapaConfigRepository).delete(config);
    }

    @Test
    @DisplayName("eliminarEtapa rechaza una etapa que no pertenece al flujo")
    void eliminarEtapa_rechazaOtroFlujo() {
        FlujoTrabajo otroFlujo = FlujoTrabajo.builder().idFlujo(2L).build();
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(otroFlujo).build();
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));

        assertThatThrownBy(() -> flujoTrabajoServicio.eliminarEtapa(1L, 5L))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(flujoEtapaConfigRepository, never()).delete(any(FlujoEtapaConfig.class));
    }

    @Test
    @DisplayName("eliminarEtapa lanza recurso no encontrado si la configuracion no existe")
    void eliminarEtapa_inexistente() {
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> flujoTrabajoServicio.eliminarEtapa(1L, 5L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }
}
