package uteq.edu.ec.artisync.specification.seguridad;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;

import java.util.ArrayList;
import java.util.List;

/**
 * Hallazgo 1.3 (INFORME-REVISION-COMPLETA.md): filtrado real en backend para
 * el listado y la exportación de usuarios — antes {@code AdminUserController.getAllUsers}
 * no aceptaba ningún filtro y la pantalla solo filtraba en memoria la página
 * cargada. Mismo patrón que {@code EventoAuditoriaSpecification}.
 */
public class UsuarioSpecification {

    private UsuarioSpecification() {
    }

    public static Specification<Usuario> conFiltros(String busqueda, String rol, Boolean estadoCuenta) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (busqueda != null && !busqueda.isBlank()) {
                String likePattern = "%" + busqueda.toLowerCase().trim() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("nombres")), likePattern),
                        cb.like(cb.lower(root.get("apellidos")), likePattern),
                        cb.like(cb.lower(root.get("correo")), likePattern)));
            }

            if (estadoCuenta != null) {
                predicados.add(cb.equal(root.get("estadoCuenta"), estadoCuenta));
            }

            if (rol != null && !rol.isBlank()) {
                // Usuario no tiene una colección de roles mapeada directamente (se
                // resuelven vía UsuarioRolRepository en el resto del código), así
                // que se filtra con un EXISTS sobre usuario_roles/roles en vez de
                // un join sobre el root.
                Subquery<Long> sub = query.subquery(Long.class);
                var ur = sub.from(UsuarioRol.class);
                Join<UsuarioRol, Rol> r = ur.join("rol");
                sub.select(ur.get("usuario").get("idUsuario"))
                        .where(cb.equal(cb.upper(r.get("nombreRol")), rol.toUpperCase()));
                predicados.add(root.get("idUsuario").in(sub));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
