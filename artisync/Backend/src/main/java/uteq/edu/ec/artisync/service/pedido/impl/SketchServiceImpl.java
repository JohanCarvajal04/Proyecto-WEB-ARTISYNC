package uteq.edu.ec.artisync.service.pedido.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.pedido.SketchResponse;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.pedido.Sketch;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.repository.pedido.SketchRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;
import uteq.edu.ec.artisync.service.pedido.ISketchService;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;
import uteq.edu.ec.artisync.service.shared.almacenamiento.FileExtensions;
import uteq.edu.ec.artisync.service.shared.almacenamiento.FilePolicy;
import uteq.edu.ec.artisync.service.shared.almacenamiento.StoragePrefix;

@Slf4j
@Service
@RequiredArgsConstructor
public class SketchServiceImpl implements ISketchService {

    private final SketchRepository bocetoRepository;
    private final OrderRepository pedidoRepository;
    private final DocumentStorage almacenamiento;
    private final NotificationService notificacionService;

    @Override
    @Transactional
    public SketchResponse subirBoceto(Long idPedido, Long idCreador, MultipartFile imagen) {
        FilePolicy.BOCETO.validar(imagen);

        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

        Long idCreadorServicio = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        if (!idCreadorServicio.equals(idCreador)) {
            throw new BusinessRuleException("Solo el creador del servicio puede subir el boceto");
        }

        Sketch boceto = bocetoRepository.findByPedidoIdPedido(idPedido)
                .orElse(Sketch.builder().pedido(pedido).build());

        // Resubir reemplaza (mismo patrón que el entregable final): la
        // referencia anterior quedaría sin nadie que la apunte, y en Azure se
        // seguiría facturando.
        String anterior = boceto.getUrlImagen();
        boceto.setUrlImagen(almacenamiento.guardar(imagen, StoragePrefix.BOCETOS));
        boceto = bocetoRepository.save(boceto);
        eliminarSiExiste(anterior);

        log.info("Boceto subido para pedido {} por creador {}", idPedido, idCreador);

        notificacionService.notify(pedido.getUsuarioCliente(), "BOCETO_SUBIDO",
                "Se subió un nuevo boceto para tu pedido \"" + pedido.getServicio().getTituloServicio() + "\".");

        return mapToRespuesta(boceto);
    }

    private void eliminarSiExiste(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return;
        }
        try {
            almacenamiento.eliminar(referencia);
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar el boceto reemplazado {}: {}", referencia, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SketchResponse obtenerBoceto(Long idPedido, Long idUsuario) {
        Sketch boceto = bocetoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No hay boceto para este pedido"));

        validarAcceso(boceto.getPedido(), idUsuario);

        return mapToRespuesta(boceto);
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoDescargado descargarBoceto(Long idPedido, Long idUsuario) {
        Sketch boceto = bocetoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No hay boceto para este pedido"));

        validarAcceso(boceto.getPedido(), idUsuario);

        String referencia = boceto.getUrlImagen();
        return new ArchivoDescargado(
                almacenamiento.leer(referencia),
                "boceto-pedido-" + idPedido + extensionDe(referencia),
                FileExtensions.contentTypeDe(referencia));
    }

    /** La ve el cliente que da seguimiento y el creador que lo subió; nadie más. */
    private void validarAcceso(Order pedido, Long idUsuario) {
        boolean esCliente = pedido.getUsuarioCliente().getIdUsuario().equals(idUsuario);
        boolean esCreador = pedido.getServicio().getPerfil().getUsuario().getIdUsuario().equals(idUsuario);
        if (!esCliente && !esCreador) {
            throw new BusinessRuleException("No tiene acceso al boceto de este pedido");
        }
    }

    private String extensionDe(String referencia) {
        int punto = referencia.lastIndexOf('.');
        return punto < 0 ? "" : referencia.substring(punto);
    }

    private SketchResponse mapToRespuesta(Sketch boceto) {
        Long idPedido = boceto.getPedido().getIdPedido();
        return SketchResponse.builder()
                .idBoceto(boceto.getIdBoceto())
                .idPedido(idPedido)
                .urlImagen(urlDescarga(boceto.getUrlImagen(), "/api/v1/pedidos/" + idPedido + "/boceto/descargar"))
                .fechaSubida(boceto.getFechaSubida())
                .build();
    }

    private String urlDescarga(String referencia, String rutaProxy) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }
        return almacenamiento.urlTemporal(referencia).orElse(rutaProxy);
    }
}
