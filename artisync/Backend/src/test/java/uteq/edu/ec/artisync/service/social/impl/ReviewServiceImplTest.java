package uteq.edu.ec.artisync.service.social.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.peticion.social.CreateReviewRequest;
import uteq.edu.ec.artisync.dto.respuesta.social.ReviewResponse;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.legal.FinalDeliverable;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.entity.social.OfferingReview;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.FinalDeliverableRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.repository.social.OfferingReviewRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Pruebas unitarias para ReviewServiceImpl.
 * RF-09: Valida creación de reseñas solo post-entrega y una por pedido.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private OfferingReviewRepository resenaServicioRepository;
    @Mock private OrderRepository pedidoRepository;
    @Mock private FinalDeliverableRepository entregableFinalRepository;

    @InjectMocks
    private ReviewServiceImpl resenaService;

    private User cliente;
    private Offering servicio;
    private Order pedido;
    private FinalDeliverable entregableLiberado;

    @BeforeEach
    void setUp() {
        cliente = User.builder()
                .idUsuario(1L).nombres("Carlos").apellidos("Ruiz")
                .correo("carlos@test.com").build();

        servicio = Offering.builder()
                .idServicio(5L).tituloServicio("Diseño de Logo").build();

        pedido = Order.builder()
                .idPedido(50L).usuarioCliente(cliente).servicio(servicio).build();

        entregableLiberado = FinalDeliverable.builder()
                .idEntregable(1L).pedido(pedido).estaLiberado(true).build();
    }

    // =========================================================================
    // crearResena
    // =========================================================================

    @Test
    @DisplayName("crearResena — crea exitosamente con entregable liberado")
    void crearResena_entregableLiberado_creaCorrectamente() {
        CreateReviewRequest peticion = CreateReviewRequest.builder()
                .calificacionEstrellas(5)
                .textoResena("Excelente trabajo")
                .build();

        OfferingReview resena = OfferingReview.builder()
                .idResena(1L).pedido(pedido)
                .calificacionEstrellas(5).textoResena("Excelente trabajo")
                .fechaResena(LocalDateTime.now()).build();

        given(pedidoRepository.findById(50L)).willReturn(Optional.of(pedido));
        given(entregableFinalRepository.findByPedidoIdPedido(50L))
                .willReturn(Optional.of(entregableLiberado));
        given(resenaServicioRepository.existsByPedidoIdPedido(50L)).willReturn(false);
        given(resenaServicioRepository.save(any(OfferingReview.class))).willReturn(resena);

        ReviewResponse resultado = resenaService.crearResena(50L, peticion, 1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCalificacionEstrellas()).isEqualTo(5);
        assertThat(resultado.getTextoResena()).isEqualTo("Excelente trabajo");
        verify(resenaServicioRepository).save(any(OfferingReview.class));
    }

    @Test
    @DisplayName("crearResena — lanza FORBIDDEN si no es el cliente del pedido")
    void crearResena_noEsCliente_lanzaForbidden() {
        given(pedidoRepository.findById(50L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> resenaService.crearResena(50L,
                new CreateReviewRequest(4, "Bien"), 999L)) // ID incorrecto
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("crearResena — lanza BusinessRuleException si entregable no está liberado")
    void crearResena_entregableNoLiberado_lanzaExcepcion() {
        entregableLiberado.setEstaLiberado(false);
        given(pedidoRepository.findById(50L)).willReturn(Optional.of(pedido));
        given(entregableFinalRepository.findByPedidoIdPedido(50L))
                .willReturn(Optional.of(entregableLiberado));

        assertThatThrownBy(() -> resenaService.crearResena(50L,
                new CreateReviewRequest(3, "Regular"), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("después de recibir el entregable");
    }

    @Test
    @DisplayName("crearResena — lanza BusinessRuleException si no hay entregable")
    void crearResena_sinEntregable_lanzaExcepcion() {
        given(pedidoRepository.findById(50L)).willReturn(Optional.of(pedido));
        given(entregableFinalRepository.findByPedidoIdPedido(50L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> resenaService.crearResena(50L,
                new CreateReviewRequest(3, "Regular"), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("después de recibir el entregable");
    }

    @Test
    @DisplayName("crearResena — lanza DuplicateResourceException si ya existe reseña")
    void crearResena_yaExisteResena_lanzaExcepcion() {
        given(pedidoRepository.findById(50L)).willReturn(Optional.of(pedido));
        given(entregableFinalRepository.findByPedidoIdPedido(50L))
                .willReturn(Optional.of(entregableLiberado));
        given(resenaServicioRepository.existsByPedidoIdPedido(50L)).willReturn(true);

        assertThatThrownBy(() -> resenaService.crearResena(50L,
                new CreateReviewRequest(5, "De nuevo"), 1L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Ya has dejado una reseña");
    }

    // =========================================================================
    // obtenerMiResena / actualizarResena / eliminarResena
    // =========================================================================

    @Test
    @DisplayName("obtenerMiResena — retorna null si el pedido no tiene reseña")
    void obtenerMiResena_sinResena_retornaNull() {
        given(resenaServicioRepository.findByPedidoIdPedido(50L)).willReturn(Optional.empty());

        ReviewResponse resultado = resenaService.obtenerMiResena(50L, 1L);

        assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("obtenerMiResena — retorna null si la reseña es de otro cliente")
    void obtenerMiResena_deOtroCliente_retornaNull() {
        OfferingReview resena = OfferingReview.builder()
                .idResena(1L).pedido(pedido).calificacionEstrellas(5).build();
        given(resenaServicioRepository.findByPedidoIdPedido(50L)).willReturn(Optional.of(resena));

        ReviewResponse resultado = resenaService.obtenerMiResena(50L, 999L);

        assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("obtenerMiResena — retorna la reseña del cliente dueño del pedido")
    void obtenerMiResena_esMiResena_retornaResena() {
        OfferingReview resena = OfferingReview.builder()
                .idResena(1L).pedido(pedido).calificacionEstrellas(5)
                .textoResena("Genial").fechaResena(LocalDateTime.now()).build();
        given(resenaServicioRepository.findByPedidoIdPedido(50L)).willReturn(Optional.of(resena));

        ReviewResponse resultado = resenaService.obtenerMiResena(50L, 1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCalificacionEstrellas()).isEqualTo(5);
    }

    @Test
    @DisplayName("actualizarResena — edita calificación y texto si es el dueño")
    void actualizarResena_esDueno_editaCorrectamente() {
        OfferingReview resena = OfferingReview.builder()
                .idResena(1L).pedido(pedido).calificacionEstrellas(3).textoResena("Ok").build();
        given(resenaServicioRepository.findByPedidoIdPedido(50L)).willReturn(Optional.of(resena));
        given(resenaServicioRepository.save(any(OfferingReview.class))).willAnswer(inv -> inv.getArgument(0));

        ReviewResponse resultado = resenaService.actualizarResena(50L,
                new CreateReviewRequest(5, "Mejoró mucho"), 1L);

        assertThat(resultado.getCalificacionEstrellas()).isEqualTo(5);
        assertThat(resultado.getTextoResena()).isEqualTo("Mejoró mucho");
    }

    @Test
    @DisplayName("actualizarResena — lanza FORBIDDEN si no es el dueño")
    void actualizarResena_noEsDueno_lanzaForbidden() {
        OfferingReview resena = OfferingReview.builder()
                .idResena(1L).pedido(pedido).calificacionEstrellas(3).build();
        given(resenaServicioRepository.findByPedidoIdPedido(50L)).willReturn(Optional.of(resena));

        assertThatThrownBy(() -> resenaService.actualizarResena(50L,
                new CreateReviewRequest(5, "Intento ajeno"), 999L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("eliminarResena — elimina si es el dueño")
    void eliminarResena_esDueno_eliminaCorrectamente() {
        OfferingReview resena = OfferingReview.builder()
                .idResena(1L).pedido(pedido).calificacionEstrellas(3).build();
        given(resenaServicioRepository.findByPedidoIdPedido(50L)).willReturn(Optional.of(resena));

        resenaService.eliminarResena(50L, 1L);

        verify(resenaServicioRepository).delete(resena);
    }

    @Test
    @DisplayName("eliminarResena — lanza FORBIDDEN si no es el dueño")
    void eliminarResena_noEsDueno_lanzaForbidden() {
        OfferingReview resena = OfferingReview.builder()
                .idResena(1L).pedido(pedido).calificacionEstrellas(3).build();
        given(resenaServicioRepository.findByPedidoIdPedido(50L)).willReturn(Optional.of(resena));

        assertThatThrownBy(() -> resenaService.eliminarResena(50L, 999L))
                .isInstanceOf(ResponseStatusException.class);
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

        List<ReviewResponse> resultado = resenaService.listarResenasPorCreador(10L);

        assertThat(resultado).isEmpty();
    }
}
