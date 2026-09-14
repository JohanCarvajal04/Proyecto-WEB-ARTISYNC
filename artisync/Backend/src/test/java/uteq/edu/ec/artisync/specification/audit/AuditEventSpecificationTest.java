package uteq.edu.ec.artisync.specification.audit;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.entity.audit.AuditEvent;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Ejercita cada rama de {@link AuditEventSpecification#conFiltros} contra un
 * {@link CriteriaBuilder}/{@link Root} simulados (deep stubs).
 */
class AuditEventSpecificationTest {

    @SuppressWarnings("unchecked")
    private final Root<AuditEvent> root = mock(Root.class, RETURNS_DEEP_STUBS);
    @SuppressWarnings("unchecked")
    private final CriteriaQuery<AuditEvent> query = mock(CriteriaQuery.class, RETURNS_DEEP_STUBS);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

    @Test
    void sinFiltros_noAplicaNingunPredicadoPropio() {
        Specification<AuditEvent> spec = AuditEventSpecification.conFiltros(
                null, null, null, null, null, null, null, null);

        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).and(new Predicate[0]);
    }

    @Test
    void correoActor_filtraPorLikeCaseInsensitive() {
        Specification<AuditEvent> spec = AuditEventSpecification.conFiltros(
                "ADMIN@artisync.com", null, null, null, null, null, null, null);

        spec.toPredicate(root, query, cb);

        verify(cb).like((Expression<String>) org.mockito.ArgumentMatchers.any(), anyString());
    }

    @Test
    void accionModuloResultadoEntidad_filtranPorIgualdadExacta() {
        Specification<AuditEvent> spec = AuditEventSpecification.conFiltros(
                null, "USUARIO_CREAR", "SEGURIDAD", "EXITO", "usuarios", null, null, null);

        spec.toPredicate(root, query, cb);

        verify(cb).equal(root.get("accionAuditoria"), "USUARIO_CREAR");
        verify(cb).equal(root.get("moduloAuditoria"), "SEGURIDAD");
        verify(cb).equal(root.get("resultadoEvento"), "EXITO");
        verify(cb).equal(root.get("entidadAfectada"), "usuarios");
    }

    @Test
    void idEntidad_filtraPorIgualdadExacta() {
        Specification<AuditEvent> spec = AuditEventSpecification.conFiltros(
                null, null, null, null, null, 42L, null, null);

        spec.toPredicate(root, query, cb);

        verify(cb).equal(root.get("idEntidadAfectada"), 42L);
    }

    @Test
    void desdeYHasta_filtranPorRangoDeFechaDelEvento() {
        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now();

        Specification<AuditEvent> spec = AuditEventSpecification.conFiltros(
                null, null, null, null, null, null, desde, hasta);

        spec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(root.get("fechaEvento"), desde);
        verify(cb).lessThanOrEqualTo(root.get("fechaEvento"), hasta);
    }
}
