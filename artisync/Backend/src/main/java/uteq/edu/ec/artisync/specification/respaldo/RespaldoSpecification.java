package uteq.edu.ec.artisync.specification.respaldo;

import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.dto.peticion.respaldo.FiltroRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.Respaldo;

public final class RespaldoSpecification {

    private RespaldoSpecification() {
    }

    public static Specification<Respaldo> conFiltro(FiltroRespaldo filtro) {
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
