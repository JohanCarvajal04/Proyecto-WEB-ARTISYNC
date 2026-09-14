package uteq.edu.ec.artisync.specification.backup;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.dto.request.backup.BackupFilter;
import uteq.edu.ec.artisync.entity.backup.Backup;
import uteq.edu.ec.artisync.entity.backup.BackupOrigin;
import uteq.edu.ec.artisync.entity.backup.BackupStatus;
import uteq.edu.ec.artisync.entity.backup.BackupType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Ejercita cada rama de {@link BackupSpecification#conFiltro} contra un
 * {@link CriteriaBuilder}/{@link Root} simulados (deep stubs).
 */
class BackupSpecificationTest {

    @SuppressWarnings("unchecked")
    private final Root<Backup> root = mock(Root.class, RETURNS_DEEP_STUBS);
    @SuppressWarnings("unchecked")
    private final CriteriaQuery<Backup> query = mock(CriteriaQuery.class, RETURNS_DEEP_STUBS);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

    @Test
    void filtroVacio_noAplicaNingunPredicadoPropio() {
        Specification<Backup> spec = BackupSpecification.conFiltro(new BackupFilter());

        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
    }

    @Test
    void tipoRespaldo_filtraPorIgualdadExacta() {
        BackupFilter filtro = new BackupFilter();
        filtro.setTipoRespaldo(BackupType.INCREMENTAL);

        BackupSpecification.conFiltro(filtro).toPredicate(root, query, cb);

        verify(cb).equal(root.get("tipoRespaldo"), BackupType.INCREMENTAL);
    }

    @Test
    void estadoRespaldo_filtraPorIgualdadExacta() {
        BackupFilter filtro = new BackupFilter();
        filtro.setEstadoRespaldo(BackupStatus.FALLIDO);

        BackupSpecification.conFiltro(filtro).toPredicate(root, query, cb);

        verify(cb).equal(root.get("estadoRespaldo"), BackupStatus.FALLIDO);
    }

    @Test
    void origen_filtraPorIgualdadExacta() {
        BackupFilter filtro = new BackupFilter();
        filtro.setOrigen(BackupOrigin.PROGRAMADO);

        BackupSpecification.conFiltro(filtro).toPredicate(root, query, cb);

        verify(cb).equal(root.get("origen"), BackupOrigin.PROGRAMADO);
    }

    @Test
    void desdeYHasta_filtranPorRangoDeFechaInicio() {
        BackupFilter filtro = new BackupFilter();
        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now();
        filtro.setDesde(desde);
        filtro.setHasta(hasta);

        BackupSpecification.conFiltro(filtro).toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(root.get("fechaInicio"), desde);
        verify(cb).lessThanOrEqualTo(root.get("fechaInicio"), hasta);
    }
}
