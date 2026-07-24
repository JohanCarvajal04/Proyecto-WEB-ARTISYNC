package uteq.edu.ec.artisync.service.social.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearResena;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaResena;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.EntregableFinal;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.social.ResenaServicio;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.EntregableFinalRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.social.ResenaServicioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Pruebas unitarias para ResenaServiceImpl.
 * RF-09: Valida creación de reseñas solo post-entrega y una por pedido.
 */
@ExtendWith(MockitoExtension.class)
class ResenaServiceImplTest {

    @Mock private ResenaServicioRepository resenaServicioRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private EntregableFinalRepository entregableFinalRepository;

    @InjectMocks
    private ResenaServiceImpl resenaService;

    private Usuario cliente;
    private Servicio servicio;
    private Pedido pedido;
    private EntregableFinal entregableLiberado;

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder()
                .idUsuario(1L).nombres("Carlos").apellidos("Ruiz")
                .correo("carlos@test.com").build();

        servicio = Servicio.builder()
                .idServicio(5L).tituloServicio("Diseño de Logo").build();

        pedido = Pedido.builder()
                .idPedido(50L).usuarioCliente(cliente).servicio(servicio).build();

        entregableLiberado = EntregableFinal.builder()
                .idEntregable(1L).pedido(pedido).estaLiberado(true).build();
    }

    // =========================================================================
    // crearResena
    // =========================================================================

    @Test
    @DisplayName("crearResena — crea exitosamente con entregable liberado")
    void crearResena_entregableLiberado_creaCorrectamente() {
        PeticionCrearResena peticion = PeticionCrearResena.builder()
                .calificacionEstrellas(5)
                .textoResena("Excelente trabajo")
                .build();

        ResenaServicio resena = ResenaServicio.builder()
                .idResena(1L).pedido(pedido)
                .calificacionEstrellas(5).textoResena("Excelente trabajo")
                .fechaResena(LocalDateTime.now()).build();

        given(pedidoRepository.findById(50L)).willReturn(Optional.of(pedido));
        given(entregableFinalRepository.findByPedidoIdPedido(50L))
                .willReturn(Optional.of(entregableLiberado));
        given(resenaServicioRepository.existsByPedidoIdPedido(50L)).willReturn(false);
        given(resenaServicioRepository.save(any(ResenaServicio.class))).willReturn(resena);

        RespuestaResena resultado = resenaService.crearResena(50L, peticion, 1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCalificacionEstrellas()).isEqualTo(5);
        assertThat(resultado.getTextoResena()).isEqualTo("Excelente trabajo");
        verify(resenaServicioRepository).save(any(ResenaServicio.class));
    }

    @Test
    @DisplayName("crearResena — lanza FORBIDDEN si no es el cliente del pedido")
    void crearResena_noEsCliente_lanzaForbidden() {
        given(pedidoRepository.findById(50L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> resenaService.crearResena(50L,
                new PeticionCrearResena(4, "Bien"), 999L)) // ID incorrecto
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("crearResena — lanza ExcepcionReglaNegocio si entregable no está liberado")
    void crearResena_entregableNoLiberado_lanzaExcepcion() {
        entregableLiberado.setEstaLiberado(false);
        given(pedidoRepository.findById(50L)).willReturn(Optional.of(pedido));
        given(entregableFinalRepository.findByPedidoIdPedido(50L))
                .willReturn(Optional.of(entregableLiberado));

        assertThatThrownBy(() -> resenaService.crearResena(50L,
                new PeticionCrearResena(3, "Regular"), 1L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("después de recibir el entregable");
    }

    @Test
    @DisplayName("crearResena — lanza ExcepcionReglaNegocio si no hay entregable")
    void crearResena_sinEntregable_lanzaExcepcion() {
        given(pedidoRepository.findById(50L)).willReturn(Optional.of(pedido));
        given(entregableFinalRepository.findByPedidoIdPedido(50L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> resenaService.crearResena(50L,
                new PeticionCrearResena(3, "Regular"), 1L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("después de recibir el entregable");
    }

    @Test
    @DisplayName("crearResena — lanza ExcepcionRecursoDuplicado si ya existe reseña")
    void crearResena_yaExisteResena_lanzaExcepcion() {
        given(pedidoRepository.findById(50L)).willReturn(Optional.of(pedido));
        given(entregableFinalRepository.findByPedidoIdPedido(50L))
                .willReturn(Optional.of(entregableLiberado));
        given(resenaServicioRepository.existsByPedidoIdPedido(50L)).willReturn(true);

        assertThatThrownBy(() -> resenaService.crearResena(50L,
                new PeticionCrearResena(5, "De nuevo"), 1L))
                .isInstanceOf(ExcepcionRecursoDuplicado.class)
                .hasMessageContaining("Ya has dejado una reseña");
    }

    // =========================================================================
    // calcularPromedioPorCreador
    // =========================================================================

    @Test
    @DisplayName("calcularPromedioPorCreador — retorna 0.0 si no hay reseñas")
    void calcularPromedioPorCreador_sinResenas_retornaCero() {
        given(resenaServicioRepository.calcularPromedioByCreadorIdPerfil(10L)).willReturn(null);

        Double promedio = resenaService.calcularPromedioPorCreador(10L);

        assertThat(promedio).isEqualTo(0.0);
    }

    @Test
    @DisplayName("calcularPromedioPorCreador — redondea a 2 decimales")
    void calcularPromedioPorCreador_conResenas_retornaPromedio() {
        given(resenaServicioRepository.calcularPromedioByCreadorIdPerfil(10L)).willReturn(4.333333);

        Double promedio = resenaService.calcularPromedioPorCreador(10L);

        assertThat(promedio).isEqualTo(4.33);
    }

    @Test
    @DisplayName("listarResenasPorCreador — retorna lista vacía si no hay reseñas")
    void listarResenasPorCreador_sinResenas_retornaListaVacia() {
        given(resenaServicioRepository.findByCreadorIdPerfil(10L)).willReturn(List.of());

        List<RespuestaResena> resultado = resenaService.listarResenasPorCreador(10L);

        assertThat(resultado).isEmpty();
    }
}
