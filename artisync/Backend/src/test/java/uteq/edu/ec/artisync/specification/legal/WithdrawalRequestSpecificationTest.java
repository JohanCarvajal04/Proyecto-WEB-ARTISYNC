package uteq.edu.ec.artisync.specification.legal;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.entity.legal.WithdrawalRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Ejercita cada rama de {@link WithdrawalRequestSpecification#conFiltros}
 * contra un {@link CriteriaBuilder}/{@link Root} simulados (deep stubs).
 */
class WithdrawalRequestSpecificationTest {

    @SuppressWarnings("unchecked")
    private final Root<WithdrawalRequest> root = mock(Root.class, RETURNS_DEEP_STUBS);
    @SuppressWarnings("unchecked")
    private final CriteriaQuery<WithdrawalRequest> query = mock(CriteriaQuery.class, RETURNS_DEEP_STUBS);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

    @Test
    void sinFiltros_noAplicaNingunPredicadoPropio() {
        Specification<WithdrawalRequest> spec = WithdrawalRequestSpecification.conFiltros(
                null, null, null, null);

        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).and(new Predicate[0]);
    }

    @Test
    void estado_filtraPorIgualdadExacta() {
        Specification<WithdrawalRequest> spec = WithdrawalRequestSpecification.conFiltros(
                "PENDIENTE", null, null, null);

        spec.toPredicate(root, query, cb);

        verify(cb).equal(root.get("estado"), "PENDIENTE");
    }

    @Test
    void idUsuarioCreador_navegaUsuarioCreador() {
        Specification<WithdrawalRequest> spec = WithdrawalRequestSpecification.conFiltros(
                null, 9L, null, null);

        assertThat(spec.toPredicate(root, query, cb)).isNotNull();
        verify(cb).equal(root.get("usuarioCreador").get("idUsuario"), 9L);
    }

    @Test
    void desdeYHasta_filtranPorRangoDeFechaDeSolicitud() {
        LocalDateTime desde = LocalDateTime.now().minusDays(7);
        LocalDateTime hasta = LocalDateTime.now();

        Specification<WithdrawalRequest> spec = WithdrawalRequestSpecification.conFiltros(
                null, null, desde, hasta);

        spec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(root.get("fechaSolicitud"), desde);
        verify(cb).lessThanOrEqualTo(root.get("fechaSolicitud"), hasta);
    }
}
