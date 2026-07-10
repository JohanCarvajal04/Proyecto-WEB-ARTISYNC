package uteq.edu.ec.artisync.specification.catalogo;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.catalogo.ServicioEtiqueta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ServicioSpecification {

    public static Specification<Servicio> conFiltros(
            Long categoriaId,
            Long subcategoriaId,
            BigDecimal precioMin,
            BigDecimal precioMax,
            List<Long> etiquetaIds,
            String textoBusqueda,
            String estadoPublicacion) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Por defecto o por parámetro filtrar por estado de publicación
            if (estadoPublicacion != null && !estadoPublicacion.isBlank()) {
                predicates.add(cb.equal(root.get("estadoPublicacion"), estadoPublicacion));
            } else {
                predicates.add(cb.equal(root.get("estadoPublicacion"), "ACTIVO"));
            }

            if (categoriaId != null) {
                predicates.add(cb.equal(
                        root.get("subcategoria").get("categoria").get("idCategoria"), categoriaId));
            }

            if (subcategoriaId != null) {
                predicates.add(cb.equal(
                        root.get("subcategoria").get("idSubcategoria"), subcategoriaId));
            }

            if (precioMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("precioBase"), precioMin));
            }

            if (precioMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("precioBase"), precioMax));
            }

            if (textoBusqueda != null && !textoBusqueda.isBlank()) {
                String likePattern = "%" + textoBusqueda.toLowerCase().trim() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("tituloServicio")), likePattern),
                        cb.like(cb.lower(root.get("descripcionDetallada")), likePattern)
                ));
            }

            if (etiquetaIds != null && !etiquetaIds.isEmpty()) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<ServicioEtiqueta> seRoot = subquery.from(ServicioEtiqueta.class);
                subquery.select(seRoot.get("servicio").get("idServicio"))
                        .where(seRoot.get("etiqueta").get("idEtiqueta").in(etiquetaIds));
                predicates.add(root.get("idServicio").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
