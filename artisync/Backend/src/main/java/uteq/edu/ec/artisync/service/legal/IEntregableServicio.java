package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEntregable;

public interface IEntregableServicio {

    RespuestaEntregable subirEntregable(Long idPedido, Long idCreador,
                                         String urlMarcaAgua, String urlLimpia);

    RespuestaEntregable obtenerEntregable(Long idPedido, Long idUsuario);

    void aprobarEntrega(Long idPedido, Long idCliente);

    byte[] descargarVersionLimpia(Long idPedido, Long idCliente);
}
