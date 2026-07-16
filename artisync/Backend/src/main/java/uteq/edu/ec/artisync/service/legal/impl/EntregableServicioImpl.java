package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEntregable;
import uteq.edu.ec.artisync.entity.legal.EntregableFinal;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;
import uteq.edu.ec.artisync.entity.legal.TransaccionPago;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.*;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.service.legal.IEntregableServicio;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntregableServicioImpl implements IEntregableServicio {

    private final EntregableFinalRepository entregableRepository;
    private final PedidoRepository pedidoRepository;
    private final PagoGarantiaRepository pagoGarantiaRepository;
    private final ContratoRepository contratoRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;

    @Override
    @Transactional
    public RespuestaEntregable subirEntregable(Long idPedido, Long idCreador,
                                                String urlMarcaAgua, String urlLimpia) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));

        // Verificar que el usuario es el creador del servicio
        Long idCreadorServicio = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        if (!idCreadorServicio.equals(idCreador)) {
            throw new ExcepcionReglaNegocio("Solo el creador del servicio puede subir entregables");
        }

        // Crear o actualizar entregable
        EntregableFinal entregable = entregableRepository.findByPedidoIdPedido(idPedido)
                .orElse(EntregableFinal.builder()
                        .pedido(pedido)
                        .estaLiberado(false)
                        .build());

        entregable.setUrlVersionMarcaAgua(urlMarcaAgua);
        entregable.setUrlVersionLimpia(urlLimpia);

        entregable = entregableRepository.save(entregable);
        log.info("Entregable subido para pedido {} por creador {}", idPedido, idCreador);

        return mapToRespuesta(entregable, false);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaEntregable obtenerEntregable(Long idPedido, Long idUsuario) {
        EntregableFinal entregable = entregableRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No hay entregable para este pedido"));

        // El creador siempre ve ambas versiones; el cliente solo la marca de agua hasta aprobación
        Pedido pedido = entregable.getPedido();
        Long idCreadorServicio = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        boolean esCreador = idCreadorServicio.equals(idUsuario);

        return mapToRespuesta(entregable, esCreador || entregable.getEstaLiberado());
    }

    @Override
    @Transactional
    public void aprobarEntrega(Long idPedido, Long idCliente) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));

        // Verificar que el usuario es el cliente
        if (!pedido.getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new ExcepcionReglaNegocio("Solo el cliente puede aprobar la entrega");
        }

        EntregableFinal entregable = entregableRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No hay entregable para este pedido"));

        if (entregable.getEstaLiberado()) {
            throw new ExcepcionReglaNegocio("El entregable ya fue aprobado");
        }

        // Liberar fondos
        var contrato = contratoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No existe contrato para el pedido"));

        var pagoOpt = pagoGarantiaRepository.findByContratoIdContrato(contrato.getIdContrato());
        if (pagoOpt.isPresent()) {
            PagoGarantia pago = pagoOpt.get();
            pago.setEstadoFondos("Liberado");
            pagoGarantiaRepository.save(pago);

            // Registrar transacciones: egreso al creador y comisión plataforma (10%)
            BigDecimal comision = pago.getMontoRetenido().multiply(BigDecimal.valueOf(0.10));
            BigDecimal pagoCreador = pago.getMontoRetenido().subtract(comision);

            transaccionPagoRepository.save(TransaccionPago.builder()
                    .pago(pago).tipoTransaccion("Egreso").monto(pagoCreador).build());
            transaccionPagoRepository.save(TransaccionPago.builder()
                    .pago(pago).tipoTransaccion("Comision").monto(comision).build());

            log.info("Fondos liberados para pedido {}: creador=${}, comision=${}", idPedido, pagoCreador, comision);
        }

        // Habilitar descarga
        entregable.setEstaLiberado(true);
        entregableRepository.save(entregable);

        log.info("Entrega aprobada para pedido {} por cliente {}", idPedido, idCliente);

        // TODO M6: Cerrar sala de chat
        // chatService.cerrarSala(idPedido);
        // TODO M6: Notificar al creador
        // notificacionService.notificar(..., "PAGO_LIBERADO", "El pago ha sido liberado por el cliente");
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] descargarVersionLimpia(Long idPedido, Long idCliente) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));

        if (!pedido.getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new ExcepcionReglaNegocio("Solo el cliente puede descargar el entregable");
        }

        EntregableFinal entregable = entregableRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No hay entregable para este pedido"));

        if (!entregable.getEstaLiberado()) {
            throw new ExcepcionReglaNegocio("El entregable no esta disponible hasta que el pago sea liberado");
        }

        // En producción, aquí se descargaría el archivo del servicio de almacenamiento (S3/R2)
        // Por ahora retornamos la URL como bytes
        log.info("Descarga de version limpia para pedido {}", idPedido);
        return entregable.getUrlVersionLimpia() != null
                ? entregable.getUrlVersionLimpia().getBytes()
                : new byte[0];
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    private RespuestaEntregable mapToRespuesta(EntregableFinal entregable, boolean mostrarVersionLimpia) {
        return RespuestaEntregable.builder()
                .idEntregable(entregable.getIdEntregable())
                .idPedido(entregable.getPedido().getIdPedido())
                .urlVersionMarcaAgua(entregable.getUrlVersionMarcaAgua())
                .urlVersionLimpia(mostrarVersionLimpia ? entregable.getUrlVersionLimpia() : null)
                .estaLiberado(entregable.getEstaLiberado())
                .build();
    }
}
