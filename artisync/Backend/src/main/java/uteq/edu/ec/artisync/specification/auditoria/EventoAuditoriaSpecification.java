package uteq.edu.ec.artisync.specification.auditoria;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.entity.auditoria.EventoAuditoria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventoAuditoriaSpecification {

    public static Specification<EventoAuditoria> conFiltros(
            String correoActor,
            String accion,
            String modulo,
            String resultado,
            String entidad,
            Long idEntidad,
            LocalDateTime desde,
            LocalDateTime hasta) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (correoActor != null && !correoActor.isBlank()) {
                String likePattern = "%" + correoActor.toLowerCase().trim() + "%";
                predicates.add(cb.like(cb.lower(root.get("correoActor")), likePattern));
            }

            if (accion != null && !accion.isBlank()) {
                predicates.add(cb.equal(root.get("accionAuditoria"), accion));
            }

            if (modulo != null && !modulo.isBlank()) {
                predicates.add(cb.equal(root.get("moduloAuditoria"), modulo));
            }

            if (resultado != null && !resultado.isBlank()) {
                predicates.add(cb.equal(root.get("resultadoEvento"), resultado));
            }

            if (entidad != null && !entidad.isBlank()) {
                predicates.add(cb.equal(root.get("entidadAfectada"), entidad));
            }

            if (idEntidad != null) {
                predicates.add(cb.equal(root.get("idEntidadAfectada"), idEntidad));
            }

            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaEvento"), desde));
            }

            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaEvento"), hasta));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
