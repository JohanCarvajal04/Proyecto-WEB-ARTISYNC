package uteq.edu.ec.artisync.specification.legal;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.entity.legal.WithdrawalRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Filtros de la cola de revisión de retiros del Auditor Financiero. */
public class WithdrawalRequestSpecification {

    /**
     * Arma la {@link Specification} de {@link WithdrawalRequest} combinando
     * (AND) solo los criterios no nulos/no vacíos recibidos.
     *
     * @param estado estado exacto de la solicitud de retiro
     * @param idUsuarioCreador si no es {@code null}, restringe a solicitudes de ese creador
     * @param desde fecha de solicitud mínima (inclusive)
     * @param hasta fecha de solicitud máxima (inclusive)
     * @return la especificación combinada, lista para {@code findAll(spec, pageable)}
     */
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
