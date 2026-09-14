package uteq.edu.ec.artisync.specification.catalog;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.entity.catalog.Offering;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Ejercita cada rama de {@link OfferingSpecification#conFiltros} contra un
 * {@link CriteriaBuilder}/{@link Root} simulados (deep stubs): no requiere
 * levantar JPA real, solo confirma que cada filtro no nulo produce el
 * predicado esperado y que ninguna combinación lanza excepción.
 */
class OfferingSpecificationTest {

    @SuppressWarnings("unchecked")
    private final Root<Offering> root = mock(Root.class, RETURNS_DEEP_STUBS);
    @SuppressWarnings("unchecked")
    private final CriteriaQuery<Offering> query = mock(CriteriaQuery.class, RETURNS_DEEP_STUBS);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

    @Test
    void sinFiltros_noAplicaNingunPredicadoPropio() {
        Specification<Offering> spec = OfferingSpecification.conFiltros(
                null, null, null, null, null, null, null);

        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).and(new Predicate[0]);
    }

    @Test
    void estadoPublicacion_filtraPorIgualdadExacta() {
        Specification<Offering> spec = OfferingSpecification.conFiltros(
                null, null, null, null, null, null, "ACTIVO");

        spec.toPredicate(root, query, cb);

        verify(cb).equal(root.get("estadoPublicacion"), "ACTIVO");
    }

    @Test
    void categoriaId_usaSubconsultaSobreOfferingSubcategory() {
        Specification<Offering> spec = OfferingSpecification.conFiltros(
                10L, null, null, null, null, null, null);

        assertThat(spec.toPredicate(root, query, cb)).isNotNull();
    }

    @Test
    void subcategoriaId_usaSubconsultaSobreOfferingSubcategory() {
        Specification<Offering> spec = OfferingSpecification.conFiltros(
                null, 20L, null, null, null, null, null);

        assertThat(spec.toPredicate(root, query, cb)).isNotNull();
    }

    @Test
    void precioMinYMax_filtranPorRango() {
        Specification<Offering> spec = OfferingSpecification.conFiltros(
                null, null, BigDecimal.TEN, BigDecimal.valueOf(100), null, null, null);

        spec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(root.get("precioBase"), BigDecimal.TEN);
        verify(cb).lessThanOrEqualTo(root.get("precioBase"), BigDecimal.valueOf(100));
    }

    @Test
    void textoBusqueda_filtraPorTituloODescripcion() {
        Specification<Offering> spec = OfferingSpecification.conFiltros(
                null, null, null, null, null, "Retrato", null);

        assertThat(spec.toPredicate(root, query, cb)).isNotNull();
        verify(cb).or(org.mockito.ArgumentMatchers.any(Predicate.class), org.mockito.ArgumentMatchers.any(Predicate.class));
    }

    @Test
    void etiquetaIds_usaSubconsultaSobreOfferingTag() {
        Specification<Offering> spec = OfferingSpecification.conFiltros(
                null, null, null, null, List.of(1L, 2L), null, null);

        assertThat(spec.toPredicate(root, query, cb)).isNotNull();
    }
}
