package uteq.edu.ec.artisync.service.social.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.social.CreateReviewRequest;
import uteq.edu.ec.artisync.dto.respuesta.social.ReviewResponse;
import uteq.edu.ec.artisync.entity.legal.FinalDeliverable;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.social.OfferingReview;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.FinalDeliverableRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.repository.social.OfferingReviewRepository;
import uteq.edu.ec.artisync.service.social.ReviewService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de reseñas de servicios.
 * RF-09: Una reseña por pedido, solo después de la liberación del entregable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final OfferingReviewRepository resenaServicioRepository;
    private final OrderRepository pedidoRepository;
    private final FinalDeliverableRepository entregableFinalRepository;

    @Override
    @Transactional
    @Auditable(accion = "RESENA_CREAR", modulo = AuditModule.SOCIAL,
            entidad = "pedidos", idEntidad = "#idPedido",
            detalle = "{calificacionEstrellas: #peticion.calificacionEstrellas}")
    /**
     * Crea la reseña de un pedido, solo posible tras la liberación de su
     * entregable final, y una única vez por pedido.
     *
     * @param idPedido identificador del pedido a reseñar
     * @param peticion calificación en estrellas y texto de la reseña
     * @param idCliente identificador del cliente que reseña
     * @return la reseña creada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     * @throws org.springframework.web.server.ResponseStatusException 403 si quien reseña no es el cliente del pedido
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el entregable aún no fue liberado
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el pedido ya tiene una reseña
     */
    public ReviewResponse createReview(Long idPedido, CreateReviewRequest peticion, Long idCliente) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado: " + idPedido));

        // Validar que es el cliente del pedido
        if (!pedido.getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el cliente del pedido puede dejar una reseña");
        }

        // Validar que el entregable fue liberado (post-entrega)
        FinalDeliverable entregable = entregableFinalRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new BusinessRuleException(
                        "Solo puedes dejar una reseña después de recibir el entregable"));

        if (!Boolean.TRUE.equals(entregable.getEstaLiberado())) {
            throw new BusinessRuleException(
                    "Solo puedes dejar una reseña después de recibir el entregable");
        }

        // Validar que no existe ya una reseña para este pedido (UNIQUE en BD)
        if (resenaServicioRepository.existsByPedidoIdPedido(idPedido)) {
            throw new DuplicateResourceException("Ya has dejado una reseña para este pedido");
        }

        OfferingReview resena = OfferingReview.builder()
                .pedido(pedido)
                .calificacionEstrellas(peticion.getCalificacionEstrellas())
                .textoResena(peticion.getTextoResena())
                .build();

        resena = resenaServicioRepository.save(resena);
        log.info("Reseña creada para pedido {} por cliente {}", idPedido, idCliente);
        return mapToResponse(resena);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idPedido identificador del pedido
     * @param idCliente identificador del cliente, para validar que la reseña sea suya
     * @return la reseña del pedido, o {@code null} si no existe o no pertenece a ese cliente
     */
    public ReviewResponse getMyReview(Long idPedido, Long idCliente) {
        return resenaServicioRepository.findByPedidoIdPedido(idPedido)
                .filter(resena -> resena.getPedido().getUsuarioCliente().getIdUsuario().equals(idCliente))
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    @Auditable(accion = "RESENA_EDITAR", modulo = AuditModule.SOCIAL,
            entidad = "pedidos", idEntidad = "#idPedido",
            detalle = "{calificacionEstrellas: #peticion.calificacionEstrellas}")
    /**
     * Edita la reseña de un pedido.
     *
     * @param idPedido identificador del pedido
     * @param peticion nueva calificación en estrellas y texto de la reseña
     * @param idCliente identificador del cliente que edita
     * @return la reseña ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no tiene reseña
     * @throws org.springframework.web.server.ResponseStatusException 403 si quien edita no es quien la dejó
     */
    public ReviewResponse updateReview(Long idPedido, CreateReviewRequest peticion, Long idCliente) {
        OfferingReview resena = resenaServicioRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Este pedido no tiene una reseña"));

        if (!resena.getPedido().getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el cliente que dejó la reseña puede editarla");
        }

        resena.setCalificacionEstrellas(peticion.getCalificacionEstrellas());
        resena.setTextoResena(peticion.getTextoResena());
        resena = resenaServicioRepository.save(resena);
        log.info("Reseña del pedido {} editada por cliente {}", idPedido, idCliente);
        return mapToResponse(resena);
    }

    @Override
    @Transactional
    @Auditable(accion = "RESENA_ELIMINAR", modulo = AuditModule.SOCIAL,
            entidad = "pedidos", idEntidad = "#idPedido")
    /**
     * Elimina la reseña de un pedido.
     *
     * @param idPedido identificador del pedido
     * @param idCliente identificador del cliente que elimina
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no tiene reseña
     * @throws org.springframework.web.server.ResponseStatusException 403 si quien elimina no es quien la dejó
     */
    public void deleteReview(Long idPedido, Long idCliente) {
        OfferingReview resena = resenaServicioRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Este pedido no tiene una reseña"));

        if (!resena.getPedido().getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el cliente que dejó la reseña puede eliminarla");
        }

        resenaServicioRepository.delete(resena);
        log.info("Reseña del pedido {} eliminada por cliente {}", idPedido, idCliente);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idPerfilCreador identificador del perfil de creador
     * @return las reseñas de todos los pedidos de ese creador
     */
    public List<ReviewResponse> listReviewsByCreator(Long idPerfilCreador) {
        return resenaServicioRepository.findByCreadorIdPerfil(idPerfilCreador)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idPerfilCreador identificador del perfil de creador
     * @return el promedio de calificaciones de ese creador, redondeado a 2 decimales; {@code 0.0} si no tiene reseñas
     */
    public Double calculateAverageByCreator(Long idPerfilCreador) {
        Double promedio = resenaServicioRepository.calcularPromedioByCreadorIdPerfil(idPerfilCreador);
        return promedio != null ? Math.round(promedio * 100.0) / 100.0 : 0.0;
    }

    // -------------------------------------------------------------------------
    private ReviewResponse mapToResponse(OfferingReview resena) {
        Order pedido = resena.getPedido();
        String nombreCliente = pedido.getUsuarioCliente().getNombres()
                + " " + pedido.getUsuarioCliente().getApellidos();
        String tituloServicio = pedido.getServicio().getTituloServicio();

        return ReviewResponse.builder()
                .idResena(resena.getIdResena())
                .calificacionEstrellas(resena.getCalificacionEstrellas())
                .textoResena(resena.getTextoResena())
                .fechaResena(resena.getFechaResena())
                .nombreCliente(nombreCliente)
                .tituloServicio(tituloServicio)
                .build();
    }
}
