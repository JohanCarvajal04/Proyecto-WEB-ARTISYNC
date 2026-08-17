package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;
import uteq.edu.ec.artisync.service.shared.almacenamiento.ExtensionesArchivo;
import uteq.edu.ec.artisync.service.shared.almacenamiento.PoliticaArchivo;
import uteq.edu.ec.artisync.service.shared.almacenamiento.PrefijoAlmacenamiento;
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
    private final AlmacenamientoDocumentos almacenamiento;

    @Override
    @Transactional
    public RespuestaEntregable subirEntregable(Long idPedido, Long idCreador,
                                                MultipartFile versionMarcaAgua, MultipartFile versionLimpia) {
        PoliticaArchivo.ENTREGABLE.validar(versionMarcaAgua);
        PoliticaArchivo.ENTREGABLE.validar(versionLimpia);

        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));

        // Verificar que el usuario es el creador del servicio
        Long idCreadorServicio = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        if (!idCreadorServicio.equals(idCreador)) {
            throw new ExcepcionReglaNegocio("Solo el creador del servicio puede subir entregables");
        }

        EntregableFinal entregable = entregableRepository.findByPedidoIdPedido(idPedido)
                .orElse(EntregableFinal.builder()
                        .pedido(pedido)
                        .estaLiberado(false)
                        .build());

        // Resubir reemplaza: las referencias anteriores quedarían sin nadie que
        // las apunte, y en Azure se seguirían facturando.
        String anteriorMarcaAgua = entregable.getUrlVersionMarcaAgua();
        String anteriorLimpia = entregable.getUrlVersionLimpia();

        entregable.setUrlVersionMarcaAgua(
                almacenamiento.guardar(versionMarcaAgua, PrefijoAlmacenamiento.ENTREGABLES));
        entregable.setUrlVersionLimpia(
                almacenamiento.guardar(versionLimpia, PrefijoAlmacenamiento.ENTREGABLES));

        entregable = entregableRepository.save(entregable);
        eliminarSiExiste(anteriorMarcaAgua);
        eliminarSiExiste(anteriorLimpia);

        log.info("Entregable subido para pedido {} por creador {}", idPedido, idCreador);

        return mapToRespuesta(entregable, false);
    }

    /**
     * El archivo viejo ya no se referencia; que no se pueda borrar no debe
     * tumbar una subida que por lo demás salió bien.
     */
    private void eliminarSiExiste(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return;
        }
        try {
            almacenamiento.eliminar(referencia);
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar el entregable reemplazado {}: {}", referencia, e.getMessage());
        }
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
    public ArchivoDescargado descargarVersionLimpia(Long idPedido, Long idCliente) {
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

        String referencia = entregable.getUrlVersionLimpia();
        if (referencia == null || referencia.isBlank()) {
            throw new ExcepcionRecursoNoEncontrado("El entregable no tiene un archivo asociado");
        }

        log.info("Descarga de version limpia para pedido {}", idPedido);
        return new ArchivoDescargado(
                almacenamiento.leer(referencia),
                "entregable-pedido-" + idPedido + extensionDe(referencia),
                ExtensionesArchivo.contentTypeDe(referencia));
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoDescargado descargarVersionMarcaAgua(Long idPedido, Long idUsuario) {
        EntregableFinal entregable = entregableRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No hay entregable para este pedido"));

        // La ve el cliente que la debe revisar y el creador que la subió; nadie más.
        Pedido pedido = entregable.getPedido();
        boolean esCliente = pedido.getUsuarioCliente().getIdUsuario().equals(idUsuario);
        boolean esCreador = pedido.getServicio().getPerfil().getUsuario().getIdUsuario().equals(idUsuario);
        if (!esCliente && !esCreador) {
            throw new ExcepcionReglaNegocio("No tiene acceso a este entregable");
        }

        String referencia = entregable.getUrlVersionMarcaAgua();
        if (referencia == null || referencia.isBlank()) {
            throw new ExcepcionRecursoNoEncontrado("El entregable no tiene version con marca de agua");
        }

        return new ArchivoDescargado(
                almacenamiento.leer(referencia),
                "vista-previa-pedido-" + idPedido + extensionDe(referencia),
                ExtensionesArchivo.contentTypeDe(referencia));
    }

    private String extensionDe(String referencia) {
        int punto = referencia.lastIndexOf('.');
        return punto < 0 ? "" : referencia.substring(punto);
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    private RespuestaEntregable mapToRespuesta(EntregableFinal entregable, boolean mostrarVersionLimpia) {
        Long idPedido = entregable.getPedido().getIdPedido();
        return RespuestaEntregable.builder()
                .idEntregable(entregable.getIdEntregable())
                .idPedido(idPedido)
                .urlVersionMarcaAgua(urlDescarga(
                        entregable.getUrlVersionMarcaAgua(),
                        "/api/v1/pedidos/" + idPedido + "/entregable/descargar/marca-agua"))
                .urlVersionLimpia(mostrarVersionLimpia
                        ? urlDescarga(entregable.getUrlVersionLimpia(),
                                "/api/v1/pedidos/" + idPedido + "/entregable/descargar")
                        : null)
                .estaLiberado(entregable.getEstaLiberado())
                .build();
    }

    /**
     * Lo que se persiste es una referencia interna, no algo que el navegador
     * pueda pedir. Con Azure se entrega un SAS firmado y el archivo viaja
     * directo desde el blob; sin él, la ruta del endpoint que sirve los bytes.
     */
    private String urlDescarga(String referencia, String rutaProxy) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }
        return almacenamiento.urlTemporal(referencia).orElse(rutaProxy);
    }
}
