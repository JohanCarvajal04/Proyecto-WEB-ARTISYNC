package uteq.edu.ec.artisync.service.pedido;

import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearFlujoTrabajo;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionEtapaConfig;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionSwapEtapas;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaFlujoTrabajo;

import java.util.List;

public interface IFlujoTrabajoServicio {

    RespuestaFlujoTrabajo crearFlujoTrabajo(Long idUsuario, PeticionCrearFlujoTrabajo peticion);

    /** puedeVerTodos: el llamador tiene FLUJO_MODERAR (o es ADMIN) — ve los flujos de todos los creadores, no solo los suyos. */
    List<RespuestaFlujoTrabajo> listarFlujosTrabajo(Long idUsuario, boolean puedeVerTodos);

    RespuestaFlujoTrabajo obtenerFlujoPorId(Long idFlujo, Long idUsuario, boolean puedeVerTodos);

    RespuestaFlujoTrabajo actualizarFlujoTrabajo(Long idFlujo, Long idUsuario, boolean puedeVerTodos, PeticionCrearFlujoTrabajo peticion);

    RespuestaFlujoTrabajo agregarEtapa(Long idFlujo, Long idUsuario, boolean puedeVerTodos, PeticionEtapaConfig peticion);

    RespuestaFlujoTrabajo actualizarEtapa(Long idFlujo, Long idFlujoEtapa, Long idUsuario, boolean puedeVerTodos, PeticionEtapaConfig peticion);

    /** Swap atómico de numeroOrden entre dos etapas — lo usa "mover etapa arriba/abajo". */
    RespuestaFlujoTrabajo intercambiarOrdenEtapas(Long idFlujo, Long idUsuario, boolean puedeVerTodos, PeticionSwapEtapas peticion);

    void eliminarEtapa(Long idFlujo, Long idFlujoEtapa, Long idUsuario, boolean puedeVerTodos);
}
