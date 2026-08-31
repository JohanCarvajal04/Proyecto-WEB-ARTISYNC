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
import uteq.edu.ec.artisync.repository.pedido.HistorialEstadoPedidoRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;

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
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HistorialEstadoPedidoRepository historialEstadoPedidoRepository;

    @InjectMocks
    private FlujoTrabajoServicioImpl flujoTrabajoServicio;

    private FlujoTrabajo flujo;
    private Usuario creador;

    @BeforeEach
    void setUp() {
        creador = Usuario.builder().idUsuario(10L).nombres("Test").build();
        flujo = FlujoTrabajo.builder().idFlujo(1L).nombreFlujo("Flujo estandar").descripcionFlujo("desc").creador(creador).build();
    }

    @Test
    @DisplayName("crearFlujoTrabajo guarda el flujo sin etapas cuando no se proporcionan")
    void crearFlujoTrabajo_sinEtapas() {
        PeticionCrearFlujoTrabajo peticion = PeticionCrearFlujoTrabajo.builder().nombreFlujo("Flujo estandar").build();
        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo estandar", 10L)).willReturn(false);
        given(usuarioRepository.findById(10L)).willReturn(Optional.of(creador));
        given(flujoTrabajoRepository.save(any(FlujoTrabajo.class))).willReturn(flujo);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        RespuestaFlujoTrabajo respuesta = flujoTrabajoServicio.crearFlujoTrabajo(10L, peticion);

        assertThat(respuesta.getNombreFlujo()).isEqualTo("Flujo estandar");
        verify(flujoEtapaConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearFlujoTrabajo rechaza un nombre duplicado")
    void crearFlujoTrabajo_rechazaDuplicado() {
        PeticionCrearFlujoTrabajo peticion = PeticionCrearFlujoTrabajo.builder().nombreFlujo("Flujo estandar").build();
        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo estandar", 10L)).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.crearFlujoTrabajo(10L, peticion))
                .isInstanceOf(ExcepcionRecursoDuplicado.class);
    }

    @Test
    @DisplayName("crearFlujoTrabajo rechaza dos etapas con el mismo nombre (sin distinguir mayusculas)")
    void crearFlujoTrabajo_rechazaEtapasConNombreRepetido() {
        PeticionEtapaConfig etapa1 = PeticionEtapaConfig.builder().nombreEtapa("Revision").numeroOrden(1).build();
        PeticionEtapaConfig etapa2 = PeticionEtapaConfig.builder().nombreEtapa(" REVISION").numeroOrden(2).build();
        PeticionCrearFlujoTrabajo peticion = PeticionCrearFlujoTrabajo.builder()
                .nombreFlujo("Flujo con etapas").etapas(List.of(etapa1, etapa2)).build();

        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo con etapas", 10L)).willReturn(false);

        assertThatThrownBy(() -> flujoTrabajoServicio.crearFlujoTrabajo(10L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("repetidas");
        verify(flujoTrabajoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearFlujoTrabajo rechaza dos etapas con el mismo numeroOrden")
    void crearFlujoTrabajo_rechazaEtapasConOrdenRepetido() {
        PeticionEtapaConfig etapa1 = PeticionEtapaConfig.builder().nombreEtapa("Revision").numeroOrden(1).build();
        PeticionEtapaConfig etapa2 = PeticionEtapaConfig.builder().nombreEtapa("Entrega").numeroOrden(1).build();
        PeticionCrearFlujoTrabajo peticion = PeticionCrearFlujoTrabajo.builder()
                .nombreFlujo("Flujo con etapas").etapas(List.of(etapa1, etapa2)).build();

        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo con etapas", 10L)).willReturn(false);

        assertThatThrownBy(() -> flujoTrabajoServicio.crearFlujoTrabajo(10L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("mismo número de orden");
        verify(flujoTrabajoRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearFlujoTrabajo crea las etapas indicadas reutilizando etapas existentes")
    void crearFlujoTrabajo_conEtapas() {
        EtapaFlujo etapa = EtapaFlujo.builder().idEtapa(1L).nombreEtapa("Revision").build();
        PeticionEtapaConfig etapaConfig = PeticionEtapaConfig.builder().nombreEtapa("Revision").numeroOrden(1).esEtapaFinal(false).build();
        PeticionCrearFlujoTrabajo peticion = PeticionCrearFlujoTrabajo.builder()
                .nombreFlujo("Flujo con etapas").etapas(List.of(etapaConfig)).build();

        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo con etapas", 10L)).willReturn(false);
        given(usuarioRepository.findById(10L)).willReturn(Optional.of(creador));
        given(flujoTrabajoRepository.save(any(FlujoTrabajo.class))).willReturn(flujo);
        given(etapaFlujoRepository.findByNombreEtapa("Revision")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.crearFlujoTrabajo(10L, peticion);

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

        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo con etapas", 10L)).willReturn(false);
        given(usuarioRepository.findById(10L)).willReturn(Optional.of(creador));
        given(flujoTrabajoRepository.save(any(FlujoTrabajo.class))).willReturn(flujo);
        given(etapaFlujoRepository.findByNombreEtapa("Entrega")).willReturn(Optional.empty());
        given(etapaFlujoRepository.save(any(EtapaFlujo.class))).willReturn(nueva);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.crearFlujoTrabajo(10L, peticion);

        verify(etapaFlujoRepository).save(any(EtapaFlujo.class));
    }

    @Test
    @DisplayName("listarFlujosTrabajo mapea solo los del creador cuando no puede ver todos")
    void listarFlujosTrabajo_mapea() {
        given(flujoTrabajoRepository.findByCreadorIdUsuario(10L)).willReturn(List.of(flujo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        assertThat(flujoTrabajoServicio.listarFlujosTrabajo(10L, false)).hasSize(1);
    }

    @Test
    @DisplayName("listarFlujosTrabajo devuelve los de todos los creadores cuando puedeVerTodos=true (FLUJO_MODERAR)")
    void listarFlujosTrabajo_puedeVerTodos_listaTodos() {
        FlujoTrabajo flujoDeOtro = FlujoTrabajo.builder().idFlujo(2L).nombreFlujo("Otro").creador(
                Usuario.builder().idUsuario(99L).nombres("Otra").apellidos("Persona").build()).build();
        given(flujoTrabajoRepository.findAllByOrderByIdFlujoAsc()).willReturn(List.of(flujo, flujoDeOtro));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(2L)).willReturn(List.of());

        List<RespuestaFlujoTrabajo> resultado = flujoTrabajoServicio.listarFlujosTrabajo(10L, true);

        assertThat(resultado).hasSize(2);
        verify(flujoTrabajoRepository, never()).findByCreadorIdUsuario(any());
    }

    @Test
    @DisplayName("obtenerFlujoPorId lanza recurso no encontrado si no existe")
    void obtenerFlujoPorId_inexistente() {
        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> flujoTrabajoServicio.obtenerFlujoPorId(1L, 10L, false))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerFlujoPorId accede al flujo de otro creador cuando puedeVerTodos=true")
    void obtenerFlujoPorId_puedeVerTodos_accedeAFlujoAjeno() {
        given(flujoTrabajoRepository.findById(1L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        RespuestaFlujoTrabajo respuesta = flujoTrabajoServicio.obtenerFlujoPorId(1L, 999L, true);

        assertThat(respuesta.getIdFlujo()).isEqualTo(1L);
        verify(flujoTrabajoRepository, never()).findByIdFlujoAndCreadorIdUsuario(any(), any());
    }

    @Test
    @DisplayName("actualizarFlujoTrabajo cambia nombre y descripcion")
    void actualizarFlujoTrabajo_cambiaDatos() {
        PeticionCrearFlujoTrabajo peticion = PeticionCrearFlujoTrabajo.builder()
                .nombreFlujo("Renombrado").descripcionFlujo("nueva desc").build();
        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        RespuestaFlujoTrabajo respuesta = flujoTrabajoServicio.actualizarFlujoTrabajo(1L, 10L, false, peticion);

        assertThat(respuesta.getNombreFlujo()).isEqualTo("Renombrado");
    }

    @Test
    @DisplayName("agregarEtapa rechaza una etapa duplicada en el flujo")
    void agregarEtapa_rechazaDuplicada() {
        EtapaFlujo etapa = EtapaFlujo.builder().idEtapa(1L).nombreEtapa("Revision").build();
        PeticionEtapaConfig peticion = PeticionEtapaConfig.builder().nombreEtapa("Revision").numeroOrden(1).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(etapaFlujoRepository.findByNombreEtapa("Revision")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.agregarEtapa(1L, 10L, false, peticion))
                .isInstanceOf(ExcepcionRecursoDuplicado.class);
    }

    @Test
    @DisplayName("agregarEtapa guarda la nueva configuracion cuando no esta duplicada")
    void agregarEtapa_guarda() {
        EtapaFlujo etapa = EtapaFlujo.builder().idEtapa(1L).nombreEtapa("Revision").build();
        PeticionEtapaConfig peticion = PeticionEtapaConfig.builder().nombreEtapa("Revision").numeroOrden(1).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(etapaFlujoRepository.findByNombreEtapa("Revision")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(1L, 1L)).willReturn(false);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        assertThat(flujoTrabajoServicio.agregarEtapa(1L, 10L, false, peticion)).isNotNull();
        verify(flujoEtapaConfigRepository).save(any(FlujoEtapaConfig.class));
    }

    @Test
    @DisplayName("agregarEtapa rechaza un numeroOrden ya usado por otra etapa del flujo")
    void agregarEtapa_rechazaOrdenDuplicado() {
        EtapaFlujo etapa = EtapaFlujo.builder().idEtapa(1L).nombreEtapa("Revision").build();
        PeticionEtapaConfig peticion = PeticionEtapaConfig.builder().nombreEtapa("Revision").numeroOrden(1).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(etapaFlujoRepository.findByNombreEtapa("Revision")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(1L, 1L)).willReturn(false);
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndNumeroOrden(1L, 1)).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.agregarEtapa(1L, 10L, false, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("número de orden");
        verify(flujoEtapaConfigRepository, never()).save(any(FlujoEtapaConfig.class));
    }

    @Test
    @DisplayName("actualizarEtapa cambia orden y marca final cuando pertenece al flujo")
    void actualizarEtapa_cambiaDatos() {
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(flujo).numeroOrden(1).esEtapaFinal(false).build();
        PeticionEtapaConfig peticion = PeticionEtapaConfig.builder().numeroOrden(2).esEtapaFinal(true).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.actualizarEtapa(1L, 5L, 10L, false, peticion);

        assertThat(config.getNumeroOrden()).isEqualTo(2);
        assertThat(config.getEsEtapaFinal()).isTrue();
    }

    @Test
    @DisplayName("actualizarEtapa rechaza una etapa que no pertenece al flujo")
    void actualizarEtapa_rechazaOtroFlujo() {
        FlujoTrabajo otroFlujo = FlujoTrabajo.builder().idFlujo(2L).build();
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(otroFlujo).build();
        PeticionEtapaConfig peticion = PeticionEtapaConfig.builder().numeroOrden(2).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));

        assertThatThrownBy(() -> flujoTrabajoServicio.actualizarEtapa(1L, 5L, 10L, false, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("actualizarEtapa rechaza un numeroOrden que ya tiene otra etapa del flujo")
    void actualizarEtapa_rechazaOrdenDuplicado() {
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(flujo).numeroOrden(1).esEtapaFinal(false).build();
        PeticionEtapaConfig peticion = PeticionEtapaConfig.builder().numeroOrden(2).esEtapaFinal(false).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndNumeroOrden(1L, 2)).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.actualizarEtapa(1L, 5L, 10L, false, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("número de orden");
        verify(flujoEtapaConfigRepository, never()).save(any(FlujoEtapaConfig.class));
    }

    @Test
    @DisplayName("actualizarEtapa permite reenviar el mismo numeroOrden (alternarEtapaFinal no cambia el orden)")
    void actualizarEtapa_mismoOrden_noValidaColision() {
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(flujo).numeroOrden(1).esEtapaFinal(false).build();
        PeticionEtapaConfig peticion = PeticionEtapaConfig.builder().numeroOrden(1).esEtapaFinal(true).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.actualizarEtapa(1L, 5L, 10L, false, peticion);

        assertThat(config.getEsEtapaFinal()).isTrue();
        verify(flujoEtapaConfigRepository, never()).existsByFlujoIdFlujoAndNumeroOrden(any(), any());
    }

    @Test
    @DisplayName("intercambiarOrdenEtapas intercambia el numeroOrden de las dos etapas")
    void intercambiarOrdenEtapas_intercambia() {
        EtapaFlujo etapaA = EtapaFlujo.builder().idEtapa(1L).nombreEtapa("Revision").build();
        EtapaFlujo etapaB = EtapaFlujo.builder().idEtapa(2L).nombreEtapa("Entrega").build();
        FlujoEtapaConfig configA = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(flujo).etapa(etapaA).numeroOrden(1).build();
        FlujoEtapaConfig configB = FlujoEtapaConfig.builder().idFlujoEtapa(6L).flujo(flujo).etapa(etapaB).numeroOrden(2).build();
        uteq.edu.ec.artisync.dto.peticion.pedido.PeticionSwapEtapas peticion =
                uteq.edu.ec.artisync.dto.peticion.pedido.PeticionSwapEtapas.builder()
                        .idFlujoEtapaA(5L).idFlujoEtapaB(6L).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(configA));
        given(flujoEtapaConfigRepository.findById(6L)).willReturn(Optional.of(configB));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.intercambiarOrdenEtapas(1L, 10L, false, peticion);

        assertThat(configA.getNumeroOrden()).isEqualTo(2);
        assertThat(configB.getNumeroOrden()).isEqualTo(1);
        verify(flujoEtapaConfigRepository).save(configA);
        verify(flujoEtapaConfigRepository).save(configB);
    }

    @Test
    @DisplayName("intercambiarOrdenEtapas rechaza intercambiar una etapa consigo misma")
    void intercambiarOrdenEtapas_rechazaMismaEtapa() {
        uteq.edu.ec.artisync.dto.peticion.pedido.PeticionSwapEtapas peticion =
                uteq.edu.ec.artisync.dto.peticion.pedido.PeticionSwapEtapas.builder()
                        .idFlujoEtapaA(5L).idFlujoEtapaB(5L).build();
        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));

        assertThatThrownBy(() -> flujoTrabajoServicio.intercambiarOrdenEtapas(1L, 10L, false, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(flujoEtapaConfigRepository, never()).findById(any());
    }

    @Test
    @DisplayName("intercambiarOrdenEtapas rechaza si una etapa no pertenece al flujo")
    void intercambiarOrdenEtapas_rechazaEtapaDeOtroFlujo() {
        FlujoTrabajo otroFlujo = FlujoTrabajo.builder().idFlujo(2L).build();
        FlujoEtapaConfig configA = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(flujo).numeroOrden(1).build();
        FlujoEtapaConfig configB = FlujoEtapaConfig.builder().idFlujoEtapa(6L).flujo(otroFlujo).numeroOrden(2).build();
        uteq.edu.ec.artisync.dto.peticion.pedido.PeticionSwapEtapas peticion =
                uteq.edu.ec.artisync.dto.peticion.pedido.PeticionSwapEtapas.builder()
                        .idFlujoEtapaA(5L).idFlujoEtapaB(6L).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(configA));
        given(flujoEtapaConfigRepository.findById(6L)).willReturn(Optional.of(configB));

        assertThatThrownBy(() -> flujoTrabajoServicio.intercambiarOrdenEtapas(1L, 10L, false, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(flujoEtapaConfigRepository, never()).save(any(FlujoEtapaConfig.class));
    }

    @Test
    @DisplayName("eliminarEtapa borra la configuracion cuando pertenece al flujo y ningun pedido esta en ella")
    void eliminarEtapa_borra() {
        EtapaFlujo etapa = EtapaFlujo.builder().idEtapa(7L).nombreEtapa("Revision").build();
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(flujo).etapa(etapa).build();
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(historialEstadoPedidoRepository.existePedidoEnEtapaActual(1L, 7L)).willReturn(false);

        flujoTrabajoServicio.eliminarEtapa(1L, 5L, 10L, false);

        verify(flujoEtapaConfigRepository).delete(config);
    }

    @Test
    @DisplayName("eliminarEtapa rechaza si hay un pedido detenido actualmente en esa etapa")
    void eliminarEtapa_rechazaConPedidoEnEtapa() {
        EtapaFlujo etapa = EtapaFlujo.builder().idEtapa(7L).nombreEtapa("Revision").build();
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(flujo).etapa(etapa).build();
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(historialEstadoPedidoRepository.existePedidoEnEtapaActual(1L, 7L)).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.eliminarEtapa(1L, 5L, 10L, false))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("pedidos actualmente detenidos");
        verify(flujoEtapaConfigRepository, never()).delete(any(FlujoEtapaConfig.class));
    }

    @Test
    @DisplayName("eliminarEtapa rechaza una etapa que no pertenece al flujo")
    void eliminarEtapa_rechazaOtroFlujo() {
        FlujoTrabajo otroFlujo = FlujoTrabajo.builder().idFlujo(2L).creador(creador).build();
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(otroFlujo).build();
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));

        assertThatThrownBy(() -> flujoTrabajoServicio.eliminarEtapa(1L, 5L, 10L, false))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(flujoEtapaConfigRepository, never()).delete(any(FlujoEtapaConfig.class));
    }

    @Test
    @DisplayName("eliminarEtapa permite borrar la etapa de un flujo ajeno cuando puedeVerTodos=true")
    void eliminarEtapa_puedeVerTodos_borraDeFlujoAjeno() {
        Usuario otroCreador = Usuario.builder().idUsuario(77L).nombres("Otro").build();
        FlujoTrabajo flujoAjeno = FlujoTrabajo.builder().idFlujo(1L).creador(otroCreador).build();
        EtapaFlujo etapa = EtapaFlujo.builder().idEtapa(7L).nombreEtapa("Revision").build();
        FlujoEtapaConfig config = FlujoEtapaConfig.builder().idFlujoEtapa(5L).flujo(flujoAjeno).etapa(etapa).build();
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(historialEstadoPedidoRepository.existePedidoEnEtapaActual(1L, 7L)).willReturn(false);

        flujoTrabajoServicio.eliminarEtapa(1L, 5L, 10L, true);

        verify(flujoEtapaConfigRepository).delete(config);
    }

    @Test
    @DisplayName("eliminarEtapa lanza recurso no encontrado si la configuracion no existe")
    void eliminarEtapa_inexistente() {
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> flujoTrabajoServicio.eliminarEtapa(1L, 5L, 10L, false))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }
}
