package uteq.edu.ec.artisync.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.response.order.SketchResponse;
import uteq.edu.ec.artisync.entity.order.Order;
import uteq.edu.ec.artisync.entity.order.Sketch;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.order.OrderRepository;
import uteq.edu.ec.artisync.repository.order.SketchRepository;
import uteq.edu.ec.artisync.service.communication.NotificationService;
import uteq.edu.ec.artisync.service.order.ISketchService;
import uteq.edu.ec.artisync.service.shared.storage.DocumentStorage;
import uteq.edu.ec.artisync.service.shared.storage.FileExtensions;
import uteq.edu.ec.artisync.service.shared.storage.FilePolicy;
import uteq.edu.ec.artisync.service.shared.storage.StoragePrefix;

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
    public SketchResponse uploadSketch(Long idPedido, Long idCreador, MultipartFile imagen) {
        FilePolicy.BOCETO.validate(imagen);

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
        boceto.setUrlImagen(almacenamiento.save(imagen, StoragePrefix.BOCETOS));
        boceto = bocetoRepository.save(boceto);
        deleteIfExists(anterior);

        log.info("Boceto subido para pedido {} por creador {}", idPedido, idCreador);

        notificacionService.notify(pedido.getUsuarioCliente(), "BOCETO_SUBIDO",
                "Se subió un nuevo boceto para tu pedido \"" + pedido.getServicio().getTituloServicio() + "\".");

        return mapToRespuesta(boceto);
    }

    private void deleteIfExists(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return;
        }
        try {
            almacenamiento.delete(referencia);
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar el boceto reemplazado {}: {}", referencia, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SketchResponse getSketch(Long idPedido, Long idUsuario) {
        Sketch boceto = bocetoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No hay boceto para este pedido"));

        validateAccess(boceto.getPedido(), idUsuario);

        return mapToRespuesta(boceto);
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedFile downloadSketch(Long idPedido, Long idUsuario) {
        Sketch boceto = bocetoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No hay boceto para este pedido"));

        validateAccess(boceto.getPedido(), idUsuario);

        String referencia = boceto.getUrlImagen();
        return new DownloadedFile(
                almacenamiento.leer(referencia),
                "boceto-pedido-" + idPedido + extensionDe(referencia),
                FileExtensions.contentTypeDe(referencia));
    }

    /** La ve el cliente que da seguimiento y el creador que lo subió; nadie más. */
    private void validateAccess(Order pedido, Long idUsuario) {
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
                .urlImagen(downloadUrl(boceto.getUrlImagen(), "/api/v1/pedidos/" + idPedido + "/boceto/descargar"))
                .fechaSubida(boceto.getFechaSubida())
                .build();
    }

    private String downloadUrl(String referencia, String rutaProxy) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }
        return almacenamiento.urlTemporal(referencia).orElse(rutaProxy);
    }
}
