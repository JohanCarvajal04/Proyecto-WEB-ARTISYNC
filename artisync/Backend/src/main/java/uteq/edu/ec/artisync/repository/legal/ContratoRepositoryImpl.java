package uteq.edu.ec.artisync.repository.legal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.respuesta.legal.FilaReporteContrato;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación Criteria API de {@link ContratoRepositoryCustom}: construye los
 * predicados condicionalmente en Java (solo se añade el predicado si el filtro
 * llegó, nunca se bindea un parámetro nulo a Postgres), igual patrón que
 * {@code EventoAuditoriaSpecification}. Se optó por esto en vez de un
 * {@code @Query} JPQL con {@code (:param is null or ...)} porque, en este JOIN
 * de 6 tablas, Postgres no lograba inferir el tipo de un parámetro cuyo único
 * uso era "$1 is null" (PSQLException: could not determine data type of
 * parameter $1) — el driver JDBC de Postgres decide el tipo de cada bind
 * durante el Parse, antes de conocer el valor, y una ocurrencia aislada de
 * "IS NULL" no le da ninguna pista.
 */
@RequiredArgsConstructor
public class ContratoRepositoryImpl implements ContratoRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public Page<FilaReporteContrato> buscarParaReporte(LocalDateTime desde, LocalDateTime hasta,
                                                         Long idPerfilCreador, Boolean soloFirmados,
                                                         Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<FilaReporteContrato> cq = cb.createQuery(FilaReporteContrato.class);
        Root<Contrato> c = cq.from(Contrato.class);
        Join<Contrato, Pedido> p = c.join("pedido");
        Join<Pedido, Servicio> s = p.join("servicio");
        Join<Pedido, Usuario> cliente = p.join("usuarioCliente");
        Join<Servicio, PerfilCreador> perfil = s.join("perfil");
        Join<PerfilCreador, Usuario> creador = perfil.join("usuario");

        cq.select(cb.construct(FilaReporteContrato.class,
                c.get("idContrato"), p.get("idPedido"), s.get("tituloServicio"),
                cb.concat(cb.concat(cliente.get("nombres"), " "), cliente.get("apellidos")),
                cb.concat(cb.concat(creador.get("nombres"), " "), creador.get("apellidos")),
                p.get("precioPactado"), c.get("limiteRevisiones"), c.get("fechaFormalizacion"),
                cb.<Boolean>selectCase().when(cb.isNotNull(c.get("hashFirmaCliente")), true).otherwise(false),
                cb.<Boolean>selectCase().when(cb.isNotNull(c.get("hashFirmaCreador")), true).otherwise(false)));

        List<Predicate> predicados = construirPredicados(cb, c, perfil, desde, hasta, idPerfilCreador, soloFirmados);
        if (!predicados.isEmpty()) {
            cq.where(predicados.toArray(new Predicate[0]));
        }
        cq.orderBy(cb.desc(c.get("fechaFormalizacion")));

        TypedQuery<FilaReporteContrato> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<FilaReporteContrato> contenido = query.getResultList();

        long total = contar(cb, desde, hasta, idPerfilCreador, soloFirmados);

        return new PageImpl<>(contenido, pageable, total);
    }

    private long contar(CriteriaBuilder cb, LocalDateTime desde, LocalDateTime hasta,
                         Long idPerfilCreador, Boolean soloFirmados) {
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Contrato> c = cq.from(Contrato.class);
        Join<Contrato, Pedido> p = c.join("pedido");
        Join<Pedido, Servicio> s = p.join("servicio");
        Join<Servicio, PerfilCreador> perfil = s.join("perfil");

        cq.select(cb.count(c));
        List<Predicate> predicados = construirPredicados(cb, c, perfil, desde, hasta, idPerfilCreador, soloFirmados);
        if (!predicados.isEmpty()) {
            cq.where(predicados.toArray(new Predicate[0]));
        }
        return entityManager.createQuery(cq).getSingleResult();
    }

    private List<Predicate> construirPredicados(CriteriaBuilder cb, Root<Contrato> c, Join<Servicio, PerfilCreador> perfil,
                                                 LocalDateTime desde, LocalDateTime hasta,
                                                 Long idPerfilCreador, Boolean soloFirmados) {
        List<Predicate> predicados = new ArrayList<>();

        if (desde != null) {
            predicados.add(cb.greaterThanOrEqualTo(c.get("fechaFormalizacion"), desde));
        }
        if (hasta != null) {
            predicados.add(cb.lessThanOrEqualTo(c.get("fechaFormalizacion"), hasta));
        }
        if (idPerfilCreador != null) {
            predicados.add(cb.equal(perfil.get("idPerfil"), idPerfilCreador));
        }
        if (soloFirmados != null) {
            if (soloFirmados) {
                predicados.add(cb.and(
                        cb.isNotNull(c.get("hashFirmaCliente")),
                        cb.isNotNull(c.get("hashFirmaCreador"))));
            } else {
                predicados.add(cb.or(
                        cb.isNull(c.get("hashFirmaCliente")),
                        cb.isNull(c.get("hashFirmaCreador"))));
            }
        }

        return predicados;
    }
}
