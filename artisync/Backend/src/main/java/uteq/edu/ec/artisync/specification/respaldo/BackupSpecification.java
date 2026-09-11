package uteq.edu.ec.artisync.specification.respaldo;

import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.dto.peticion.respaldo.BackupFilter;
import uteq.edu.ec.artisync.entity.respaldo.Backup;

public final class BackupSpecification {

    private BackupSpecification() {
    }

    public static Specification<Backup> conFiltro(BackupFilter filtro) {
        return (root, query, cb) -> {
            var predicados = cb.conjunction();

            if (filtro.getTipoRespaldo() != null) {
                predicados = cb.and(predicados, cb.equal(root.get("tipoRespaldo"), filtro.getTipoRespaldo()));
            }
            if (filtro.getEstadoRespaldo() != null) {
                predicados = cb.and(predicados, cb.equal(root.get("estadoRespaldo"), filtro.getEstadoRespaldo()));
            }
            if (filtro.getOrigen() != null) {
                predicados = cb.and(predicados, cb.equal(root.get("origen"), filtro.getOrigen()));
            }
            if (filtro.getDesde() != null) {
                predicados = cb.and(predicados, cb.greaterThanOrEqualTo(root.get("fechaInicio"), filtro.getDesde()));
            }
            if (filtro.getHasta() != null) {
                predicados = cb.and(predicados, cb.lessThanOrEqualTo(root.get("fechaInicio"), filtro.getHasta()));
            }
            return predicados;
        };
    }
}
