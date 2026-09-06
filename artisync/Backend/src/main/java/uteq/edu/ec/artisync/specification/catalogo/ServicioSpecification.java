package uteq.edu.ec.artisync.specification.catalogo;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.catalogo.ServicioEtiqueta;
import uteq.edu.ec.artisync.entity.catalogo.ServicioSubcategoria;

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

            // estadoPublicacion == null: sin filtrar por estado (lo usa la
            // moderación de servicios, que necesita ver cualquier estado). El
            // catálogo público sigue pasando "ACTIVO" explícito.
            if (estadoPublicacion != null && !estadoPublicacion.isBlank()) {
                predicates.add(cb.equal(root.get("estadoPublicacion"), estadoPublicacion));
            }

            if (categoriaId != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<ServicioSubcategoria> ssRoot = subquery.from(ServicioSubcategoria.class);
                subquery.select(ssRoot.get("servicio").get("idServicio"))
                        .where(cb.equal(ssRoot.get("subcategoria").get("categoria").get("idCategoria"), categoriaId));
                predicates.add(root.get("idServicio").in(subquery));
            }

            if (subcategoriaId != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<ServicioSubcategoria> ssRoot = subquery.from(ServicioSubcategoria.class);
                subquery.select(ssRoot.get("servicio").get("idServicio"))
                        .where(cb.equal(ssRoot.get("subcategoria").get("idSubcategoria"), subcategoriaId));
                predicates.add(root.get("idServicio").in(subquery));
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
