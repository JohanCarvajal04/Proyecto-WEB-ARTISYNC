package uteq.edu.ec.artisync.specification.legal;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.entity.legal.EscrowPayment;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Ejercita cada rama de {@link EscrowPaymentSpecification#conFiltros} contra
 * un {@link CriteriaBuilder}/{@link Root} simulados (deep stubs).
 */
class EscrowPaymentSpecificationTest {

    @SuppressWarnings("unchecked")
    private final Root<EscrowPayment> root = mock(Root.class, RETURNS_DEEP_STUBS);
    @SuppressWarnings("unchecked")
    private final CriteriaQuery<EscrowPayment> query = mock(CriteriaQuery.class, RETURNS_DEEP_STUBS);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

    @Test
    void sinFiltros_noAplicaNingunPredicadoPropio() {
        Specification<EscrowPayment> spec = EscrowPaymentSpecification.conFiltros(
                null, null, null, null, null);

        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).and(new Predicate[0]);
    }

    @Test
    void estadoFondos_filtraPorIgualdadExacta() {
        Specification<EscrowPayment> spec = EscrowPaymentSpecification.conFiltros(
                "RETENIDO", null, null, null, null);

        spec.toPredicate(root, query, cb);

        verify(cb).equal(root.get("estadoFondos"), "RETENIDO");
    }

    @Test
    void idPerfilCreador_navegaContratoPedidoServicioPerfil() {
        Specification<EscrowPayment> spec = EscrowPaymentSpecification.conFiltros(
                null, 5L, null, null, null);

        assertThat(spec.toPredicate(root, query, cb)).isNotNull();
        verify(cb).equal(
                root.get("contrato").get("pedido").get("servicio").get("perfil").get("idPerfil"), 5L);
    }

    @Test
    void idUsuarioCliente_navegaContratoPedidoUsuarioCliente() {
        Specification<EscrowPayment> spec = EscrowPaymentSpecification.conFiltros(
                null, null, 7L, null, null);

        assertThat(spec.toPredicate(root, query, cb)).isNotNull();
        verify(cb).equal(root.get("contrato").get("pedido").get("usuarioCliente").get("idUsuario"), 7L);
    }

    @Test
    void desdeYHasta_filtranPorRangoDeFechaDeFormalizacion() {
        LocalDateTime desde = LocalDateTime.now().minusDays(30);
        LocalDateTime hasta = LocalDateTime.now();

        Specification<EscrowPayment> spec = EscrowPaymentSpecification.conFiltros(
                null, null, null, desde, hasta);

        spec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(root.get("contrato").get("fechaFormalizacion"), desde);
        verify(cb).lessThanOrEqualTo(root.get("contrato").get("fechaFormalizacion"), hasta);
    }
}
