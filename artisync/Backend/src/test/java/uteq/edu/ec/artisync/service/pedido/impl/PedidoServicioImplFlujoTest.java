package uteq.edu.ec.artisync.service.pedido.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPedido;
import uteq.edu.ec.artisync.entity.catalogo.Categoria;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.catalogo.Subcategoria;
import uteq.edu.ec.artisync.entity.pedido.EtapaFlujo;
import uteq.edu.ec.artisync.entity.pedido.FlujoEtapaConfig;
import uteq.edu.ec.artisync.entity.pedido.HistorialEstadoPedido;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.FlujoTrabajoRepository;
import uteq.edu.ec.artisync.repository.catalogo.ServicioRepository;
import uteq.edu.ec.artisync.repository.pedido.FlujoEtapaConfigRepository;
import uteq.edu.ec.artisync.repository.pedido.HistorialEstadoPedidoRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * RF-19: el flujo del pedido sale de la categoría del servicio.
 *
 * <p>Antes se tomaba {@code flujoTrabajoRepository.findAll().get(0)}: todos los
 * pedidos compartían flujo y cuál era dependía del orden que devolviese
 * Postgres. Estas pruebas fijan el comportamiento nuevo.
 */
@ExtendWith(MockitoExtension.class)
class PedidoServicioImplFlujoTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private FlujoTrabajoRepository flujoTrabajoRepository;
    @Mock private FlujoEtapaConfigRepository flujoEtapaConfigRepository;
    @Mock private HistorialEstadoPedidoRepository historialRepository;
    @Mock private uteq.edu.ec.artisync.repository.pedido.EtapaFlujoRepository etapaFlujoRepository;

    @InjectMocks
    private PedidoServicioImpl pedidoServicio;

    private Usuario cliente;
    private Servicio servicio;
    private Categoria categoria;
    private FlujoTrabajo flujoDeCategoria;
    private FlujoTrabajo flujoPorDefecto;
    private PeticionCrearPedido peticion;

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder().idUsuario(1L).build();

        Usuario creador = Usuario.builder().idUsuario(2L).build();
        PerfilCreador perfil = PerfilCreador.builder().idPerfil(10L).usuario(creador).build();

        categoria = Categoria.builder()
                .idCategoria(5L)
                .nombreCategoria("Ilustracion")
                .estadoActiva(true)
                .build();

        Subcategoria subcategoria = Subcategoria.builder()
                .idSubcategoria(7L)
                .categoria(categoria)
                .nombreSubcategoria("Personajes")
                .build();

        servicio = Servicio.builder()
                .idServicio(100L)
                .perfil(perfil)
                .subcategoria(subcategoria)
                .precioBase(new BigDecimal("50.00"))
                .build();

        flujoDeCategoria = FlujoTrabajo.builder().idFlujo(20L).nombreFlujo("Flujo ilustracion").build();
        flujoPorDefecto = FlujoTrabajo.builder().idFlujo(1L).nombreFlujo("Flujo estandar").build();

        peticion = PeticionCrearPedido.builder().idServicio(100L).build();

        lenient().when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        lenient().when(servicioRepository.findById(100L)).thenReturn(Optional.of(servicio));
        lenient().when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(historialRepository.save(any(HistorialEstadoPedido.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    /** Configura etapas para el flujo indicado, que es lo que exige crearPedido. */
    private void conEtapas(FlujoTrabajo flujo) {
        EtapaFlujo etapa = EtapaFlujo.builder().idEtapa(1L).nombreEtapa("Briefing").build();
        FlujoEtapaConfig config = FlujoEtapaConfig.builder()
                .idFlujoEtapa(1L).flujo(flujo).etapa(etapa).numeroOrden(1).esEtapaFinal(false)
                .build();
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(flujo.getIdFlujo()))
                .willReturn(List.of(config));
    }

    @Test
    @DisplayName("usa el flujo configurado en la categoria del servicio")
    void usaElFlujoDeLaCategoria() {
        categoria.setFlujo(flujoDeCategoria);
        conEtapas(flujoDeCategoria);

        pedidoServicio.crearPedido(1L, peticion);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getFlujo().getIdFlujo()).isEqualTo(20L);
    }

    @Test
    @DisplayName("cae al flujo de respaldo cuando la categoria no tiene flujo asignado")
    void caeAlFlujoDeRespaldo() {
        categoria.setFlujo(null);
        conEtapas(flujoPorDefecto);
        given(flujoTrabajoRepository.findFirstByOrderByIdFlujoAsc())
                .willReturn(Optional.of(flujoPorDefecto));

        pedidoServicio.crearPedido(1L, peticion);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getFlujo().getIdFlujo()).isEqualTo(1L);
    }

    @Test
    @DisplayName("el respaldo no depende del nombre del flujo, que lo fija el seed")
    void respaldoIndependienteDelNombre() {
        categoria.setFlujo(null);
        FlujoTrabajo conOtroNombre = FlujoTrabajo.builder()
                .idFlujo(3L).nombreFlujo("Flujo Estándar de Medición").build();
        conEtapas(conOtroNombre);
        given(flujoTrabajoRepository.findFirstByOrderByIdFlujoAsc()).willReturn(Optional.of(conOtroNombre));

        pedidoServicio.crearPedido(1L, peticion);

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getFlujo().getIdFlujo()).isEqualTo(3L);
    }

    @Test
    @DisplayName("sin ningun flujo configurado, se rechaza el pedido")
    void sinFlujosRechaza() {
        categoria.setFlujo(null);
        given(flujoTrabajoRepository.findFirstByOrderByIdFlujoAsc()).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.crearPedido(1L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("No hay flujos de trabajo configurados");
    }

    @Test
    @DisplayName("un flujo sin etapas configuradas se rechaza nombrando el flujo")
    void flujoSinEtapasRechaza() {
        categoria.setFlujo(flujoDeCategoria);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(anyLong()))
                .willReturn(List.of());

        assertThatThrownBy(() -> pedidoServicio.crearPedido(1L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("Flujo ilustracion");
    }
}
