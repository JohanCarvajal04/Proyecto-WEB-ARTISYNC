package uteq.edu.ec.artisync.service.legal;

import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEntregable;

public interface IEntregableServicio {

    RespuestaEntregable subirEntregable(Long idPedido, Long idCreador,
                                         MultipartFile versionMarcaAgua, MultipartFile versionLimpia);

    RespuestaEntregable obtenerEntregable(Long idPedido, Long idUsuario);

    void aprobarEntrega(Long idPedido, Long idCliente);

    ArchivoDescargado descargarVersionLimpia(Long idPedido, Long idCliente);

    /**
     * La versión con marca de agua es la que el cliente revisa antes de aprobar,
     * así que no depende de que los fondos estén liberados.
     */
    ArchivoDescargado descargarVersionMarcaAgua(Long idPedido, Long idUsuario);

    /**
     * Bytes del entregable junto al tipo que declara, para que el controlador
     * responda con un Content-Type correcto en vez de octet-stream genérico.
     */
    record ArchivoDescargado(byte[] contenido, String nombreSugerido, String contentType) {
    }
}
