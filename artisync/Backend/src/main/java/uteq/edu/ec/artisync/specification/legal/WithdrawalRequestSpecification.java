package uteq.edu.ec.artisync.specification.legal;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.entity.legal.WithdrawalRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Filtros de la cola de revisión de retiros del Auditor Financiero. */
public class WithdrawalRequestSpecification {

    public static Specification<WithdrawalRequest> conFiltros(
            String estado,
            Long idUsuarioCreador,
            LocalDateTime desde,
            LocalDateTime hasta) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (estado != null && !estado.isBlank()) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }

            if (idUsuarioCreador != null) {
                predicates.add(cb.equal(root.get("usuarioCreador").get("idUsuario"), idUsuarioCreador));
            }

            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaSolicitud"), desde));
            }

            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaSolicitud"), hasta));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
