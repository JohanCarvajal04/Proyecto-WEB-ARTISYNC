package uteq.edu.ec.artisync.service.pedido;

import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionAvanzarEtapa;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPedido;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPropuestaTerminos;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaHistorialEstado;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedidoResumido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPropuestaTerminos;
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
     * Propone un precio y/o fecha de entrega final, negociados por chat,
     * antes de que el contrato tenga alguna firma. Puede llamarlo el cliente
     * o el creador del pedido. El cambio no se aplica al pedido hasta que la
     * contraparte lo acepte con {@link #aceptarPropuestaTerminos}.
     */
    RespuestaPropuestaTerminos proponerTerminos(Long idPedido, Long idUsuario, PeticionCrearPropuestaTerminos peticion);

    /**
     * Solo la contraparte del proponente puede aceptar. Aplica los valores
     * propuestos al pedido y, si el pedido aún no tiene contrato, lo genera
     * ya con esos valores como términos finales.
     */
    RespuestaPedido aceptarPropuestaTerminos(Long idPedido, Long idPropuesta, Long idUsuario);

    /** Solo la contraparte del proponente puede rechazar. No modifica el pedido. */
    RespuestaPropuestaTerminos rechazarPropuestaTerminos(Long idPedido, Long idPropuesta, Long idUsuario);

    /** Solo el propio proponente puede cancelar su propuesta pendiente. */
    RespuestaPropuestaTerminos cancelarPropuestaTerminos(Long idPedido, Long idPropuesta, Long idUsuario);

    /** Lanza ExcepcionRecursoNoEncontrado si no hay ninguna propuesta pendiente. */
    RespuestaPropuestaTerminos obtenerPropuestaPendiente(Long idPedido, Long idUsuarioSolicitante);

    List<RespuestaHistorialEstado> obtenerHistorial(Long idPedido, Long idUsuarioSolicitante);

    RespuestaSeguimientoPedido obtenerSeguimiento(Long idPedido, Long idUsuarioSolicitante);
}
