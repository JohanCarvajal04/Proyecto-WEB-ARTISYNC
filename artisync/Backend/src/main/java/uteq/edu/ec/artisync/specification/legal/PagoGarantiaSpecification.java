package uteq.edu.ec.artisync.specification.legal;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtros del panel de supervisión de Pagos y Garantías (Escrow) del Auditor
 * Financiero. Navega contrato → pedido para llegar a cliente y creador, igual
 * que hace {@code fn_reporte_comisiones_creador} para el reporte agregado.
 */
public class PagoGarantiaSpecification {

    public static Specification<PagoGarantia> conFiltros(
            String estadoFondos,
            Long idPerfilCreador,
            Long idUsuarioCliente,
            LocalDateTime desde,
            LocalDateTime hasta) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (estadoFondos != null && !estadoFondos.isBlank()) {
                predicates.add(cb.equal(root.get("estadoFondos"), estadoFondos));
            }

            if (idPerfilCreador != null) {
                predicates.add(cb.equal(
                        root.get("contrato").get("pedido").get("servicio").get("perfil").get("idPerfil"),
                        idPerfilCreador));
            }

            if (idUsuarioCliente != null) {
                predicates.add(cb.equal(
                        root.get("contrato").get("pedido").get("usuarioCliente").get("idUsuario"),
                        idUsuarioCliente));
            }

            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("contrato").get("fechaFormalizacion"), desde));
            }

            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("contrato").get("fechaFormalizacion"), hasta));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
