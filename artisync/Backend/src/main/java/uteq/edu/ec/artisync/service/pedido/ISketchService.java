package uteq.edu.ec.artisync.service.pedido;

import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.pedido.SketchResponse;

public interface ISketchService {

    /**
     * Sube el boceto/avance de un pedido, con la marca de agua ya aplicada
     * por el creador fuera de la plataforma. Es un singleton por pedido: si
     * ya existe uno, lo reemplaza (mismo patrón que el entregable final).
     *
     * @param idPedido  id del pedido
     * @param idCreador id del usuario que sube el boceto, debe ser el creador del servicio
     * @param imagen    imagen con marca de agua a mostrar al cliente
     * @return el boceto recién guardado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no es el creador del servicio
     */
    SketchResponse subirBoceto(Long idPedido, Long idCreador, MultipartFile imagen);

    /**
     * Obtiene el boceto vigente de un pedido.
     *
     * @param idPedido  id del pedido
     * @param idUsuario id del usuario que consulta, debe ser cliente o creador del pedido
     * @return el boceto vigente
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no tiene boceto
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no tiene acceso al pedido
     */
    SketchResponse obtenerBoceto(Long idPedido, Long idUsuario);

    /**
     * Descarga la imagen del boceto vigente.
     *
     * @param idPedido  id del pedido
     * @param idUsuario id del usuario que descarga, debe ser cliente o creador del pedido
     * @return el archivo del boceto
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no tiene boceto
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no tiene acceso al pedido
     */
    ArchivoDescargado descargarBoceto(Long idPedido, Long idUsuario);

    record ArchivoDescargado(byte[] contenido, String nombreSugerido, String contentType) {
    }
}
