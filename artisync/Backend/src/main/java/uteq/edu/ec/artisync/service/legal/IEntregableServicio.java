package uteq.edu.ec.artisync.service.legal;

import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEntregable;

public interface IEntregableServicio {

    /**
     * Sube el entregable de un pedido: una versión con marca de agua para revisión
     * del cliente y la versión limpia final. Solo el creador del servicio puede subirlo.
     *
     * @param idPedido         id del pedido
     * @param idCreador        id del usuario que sube el entregable, debe ser el creador del servicio
     * @param versionMarcaAgua archivo con marca de agua, visible antes de la aprobación
     * @param versionLimpia    archivo final, visible tras la aprobación y liberación del pago
     * @return el entregable recién registrado
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el pedido no existe
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el solicitante no es el creador del servicio
     */
    RespuestaEntregable subirEntregable(Long idPedido, Long idCreador,
                                         MultipartFile versionMarcaAgua, MultipartFile versionLimpia);

    /**
     * Obtiene el detalle del entregable de un pedido.
     *
     * @param idPedido  id del pedido
     * @param idUsuario id del usuario que consulta
     * @return el detalle del entregable
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el pedido no tiene entregable
     */
    RespuestaEntregable obtenerEntregable(Long idPedido, Long idUsuario);

    /**
     * Aprueba el entregable de un pedido. Solo el cliente puede aprobarlo, y solo una vez;
     * la aprobación es la señal que libera el pago en garantía (ver {@code IPagoServicio}).
     *
     * @param idPedido  id del pedido
     * @param idCliente id del usuario que aprueba, debe ser el cliente del pedido
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el pedido, el entregable o su contrato no existen
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el solicitante no es el cliente del pedido, o si el entregable ya fue aprobado
     */
    void aprobarEntrega(Long idPedido, Long idCliente);

    /**
     * Descarga la versión limpia (final) del entregable. Solo disponible para el cliente,
     * y solo después de que el pago en garantía haya sido liberado.
     *
     * @param idPedido  id del pedido
     * @param idCliente id del usuario que descarga, debe ser el cliente del pedido
     * @return el archivo final del entregable
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el pedido no tiene entregable, o si el entregable no tiene un archivo asociado
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el solicitante no es el cliente del pedido, o si el pago aún no ha sido liberado
     */
    ArchivoDescargado descargarVersionLimpia(Long idPedido, Long idCliente);

    /**
     * La versión con marca de agua es la que el cliente revisa antes de aprobar,
     * así que no depende de que los fondos estén liberados.
     *
     * @param idPedido  id del pedido
     * @param idUsuario id del usuario que descarga, debe ser cliente o creador del pedido
     * @return el archivo con marca de agua del entregable
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el pedido no tiene entregable, o si el entregable no tiene versión con marca de agua
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el solicitante no tiene acceso a este entregable
     */
    ArchivoDescargado descargarVersionMarcaAgua(Long idPedido, Long idUsuario);

    /**
     * Bytes del entregable junto al tipo que declara, para que el controlador
     * responda con un Content-Type correcto en vez de octet-stream genérico.
     */
    record ArchivoDescargado(byte[] contenido, String nombreSugerido, String contentType) {
    }
}
