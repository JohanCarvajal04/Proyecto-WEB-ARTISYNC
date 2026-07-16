package uteq.edu.ec.artisync.service.pedido;

import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionAvanzarEtapa;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPedido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaHistorialEstado;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedidoResumido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaSeguimientoPedido;

import java.util.List;

public interface IPedidoServicio {

    RespuestaPedido crearPedido(Long idCliente, PeticionCrearPedido peticion);

    RespuestaPedido obtenerPedidoPorId(Long idPedido);

    List<RespuestaPedidoResumido> listarMisPedidos(Long idCliente);

    List<RespuestaPedidoResumido> listarMisComisiones(Long idCreador);

    RespuestaPedido avanzarEtapa(Long idPedido, Long idCreador, PeticionAvanzarEtapa peticion);

    List<RespuestaHistorialEstado> obtenerHistorial(Long idPedido);

    RespuestaSeguimientoPedido obtenerSeguimiento(Long idPedido);
}
