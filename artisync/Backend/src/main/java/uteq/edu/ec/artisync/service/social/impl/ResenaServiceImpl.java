package uteq.edu.ec.artisync.service.social.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearResena;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaResena;
import uteq.edu.ec.artisync.entity.legal.EntregableFinal;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.social.ResenaServicio;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.EntregableFinalRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
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
    private final PedidoRepository pedidoRepository;
    private final EntregableFinalRepository entregableFinalRepository;

    @Override
    @Transactional
    public RespuestaResena crearResena(Long idPedido, PeticionCrearResena peticion, Long idCliente) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado: " + idPedido));

        // Validar que es el cliente del pedido
        if (!pedido.getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el cliente del pedido puede dejar una reseña");
        }

        // Validar que el entregable fue liberado (post-entrega)
        EntregableFinal entregable = entregableFinalRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionReglaNegocio(
                        "Solo puedes dejar una reseña después de recibir el entregable"));

        if (!Boolean.TRUE.equals(entregable.getEstaLiberado())) {
            throw new ExcepcionReglaNegocio(
                    "Solo puedes dejar una reseña después de recibir el entregable");
        }

        // Validar que no existe ya una reseña para este pedido (UNIQUE en BD)
        if (resenaServicioRepository.existsByPedidoIdPedido(idPedido)) {
            throw new ExcepcionRecursoDuplicado("Ya has dejado una reseña para este pedido");
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
    public List<RespuestaResena> listarResenasPorCreador(Long idPerfilCreador) {
        return resenaServicioRepository.findByCreadorIdPerfil(idPerfilCreador)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Double calcularPromedioPorCreador(Long idPerfilCreador) {
        Double promedio = resenaServicioRepository.calcularPromedioByCreadorIdPerfil(idPerfilCreador);
        return promedio != null ? Math.round(promedio * 100.0) / 100.0 : 0.0;
    }

    // -------------------------------------------------------------------------
    private RespuestaResena mapToResponse(ResenaServicio resena) {
        Pedido pedido = resena.getPedido();
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
