package uteq.edu.ec.artisync.service.pedido;

import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionActualizarTerminosPedido;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionAvanzarEtapa;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPedido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaHistorialEstado;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedidoResumido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaSeguimientoPedido;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;

import java.util.List;

public interface IPedidoServicio {

    RespuestaPedido crearPedido(Long idCliente, PeticionCrearPedido peticion);

    RespuestaPedido obtenerPedidoPorId(Long idPedido, Long idUsuarioSolicitante);

    List<RespuestaPedidoResumido> listarMisPedidos(Long idCliente);

    List<RespuestaPedidoResumido> listarMisComisiones(Long idCreador);

    /**
     * Exportación "propia": el permiso lo da ya tener sesión como el cliente
     * dueño de estos pedidos, no hay un permiso de exportación aparte (a
     * diferencia de auditoría/finanzas/contratos, que son reportes
     * administrativos transversales).
     */
    DocumentoGenerado exportarMisPedidos(Long idCliente, FormatoReporte formato, String correoSolicitante);

    /**
     * 1.4 (INFORME-REVISION-COMPLETA.md): {@code idsPedido} nulo o vacío exporta
     * todas las comisiones (compatibilidad); si llega con valores, se exportan
     * solo esos pedidos — el frontend manda ahí los ids ya visibles tras
     * aplicar sus filtros de pantalla (estado/etapa/búsqueda), así se garantiza
     * "se exporta lo que se ve" sin duplicar esa lógica de filtrado en Java.
     */
    DocumentoGenerado exportarMisComisiones(Long idCreador, List<Long> idsPedido, FormatoReporte formato, String correoSolicitante);

    RespuestaPedido avanzarEtapa(Long idPedido, Long idCreador, PeticionAvanzarEtapa peticion);

    /**
     * Ajusta precio y/o fecha de entrega mientras se negocia por chat, antes
     * de que el contrato tenga alguna firma. Puede llamarlo el cliente o el
     * creador del pedido.
     */
    RespuestaPedido actualizarTerminos(Long idPedido, Long idUsuario, PeticionActualizarTerminosPedido peticion);

    List<RespuestaHistorialEstado> obtenerHistorial(Long idPedido, Long idUsuarioSolicitante);

    RespuestaSeguimientoPedido obtenerSeguimiento(Long idPedido, Long idUsuarioSolicitante);
}
