package uteq.edu.ec.artisync.service.social.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearResena;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaResena;
import uteq.edu.ec.artisync.entity.legal.FinalDeliverable;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.social.ResenaServicio;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.FinalDeliverableRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.repository.social.ResenaServicioRepository;
import uteq.edu.ec.artisync.service.social.ResenaService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de reseñas de servicios.
 * RF-09: Una reseña por pedido, solo después de la liberación del entregable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResenaServiceImpl implements ResenaService {

    private final ResenaServicioRepository resenaServicioRepository;
    private final OrderRepository pedidoRepository;
    private final FinalDeliverableRepository entregableFinalRepository;

    @Override
    @Transactional
    @Auditable(accion = "RESENA_CREAR", modulo = AuditModule.SOCIAL,
            entidad = "pedidos", idEntidad = "#idPedido",
            detalle = "{calificacionEstrellas: #peticion.calificacionEstrellas}")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @param idCliente identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaResena crearResena(Long idPedido, PeticionCrearResena peticion, Long idCliente) {
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

        ResenaServicio resena = ResenaServicio.builder()
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
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idCliente identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaResena obtenerMiResena(Long idPedido, Long idCliente) {
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
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @param idCliente identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaResena actualizarResena(Long idPedido, PeticionCrearResena peticion, Long idCliente) {
        ResenaServicio resena = resenaServicioRepository.findByPedidoIdPedido(idPedido)
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
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idCliente identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void eliminarResena(Long idPedido, Long idCliente) {
        ResenaServicio resena = resenaServicioRepository.findByPedidoIdPedido(idPedido)
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
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idPerfilCreador identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaResena> listarResenasPorCreador(Long idPerfilCreador) {
        return resenaServicioRepository.findByCreadorIdPerfil(idPerfilCreador)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idPerfilCreador identificador unico que referencia de manera univoca al registro
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Double calcularPromedioPorCreador(Long idPerfilCreador) {
        Double promedio = resenaServicioRepository.calcularPromedioByCreadorIdPerfil(idPerfilCreador);
        return promedio != null ? Math.round(promedio * 100.0) / 100.0 : 0.0;
    }

    // -------------------------------------------------------------------------
    private RespuestaResena mapToResponse(ResenaServicio resena) {
        Order pedido = resena.getPedido();
        String nombreCliente = pedido.getUsuarioCliente().getNombres()
                + " " + pedido.getUsuarioCliente().getApellidos();
        String tituloServicio = pedido.getServicio().getTituloServicio();

        return RespuestaResena.builder()
                .idResena(resena.getIdResena())
                .calificacionEstrellas(resena.getCalificacionEstrellas())
                .textoResena(resena.getTextoResena())
                .fechaResena(resena.getFechaResena())
                .nombreCliente(nombreCliente)
                .tituloServicio(tituloServicio)
                .build();
    }
}
