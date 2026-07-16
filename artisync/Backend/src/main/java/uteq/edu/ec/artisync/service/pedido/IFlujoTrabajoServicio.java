package uteq.edu.ec.artisync.service.pedido;

import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearFlujoTrabajo;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionEtapaConfig;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaFlujoTrabajo;

import java.util.List;

public interface IFlujoTrabajoServicio {

    RespuestaFlujoTrabajo crearFlujoTrabajo(PeticionCrearFlujoTrabajo peticion);

    List<RespuestaFlujoTrabajo> listarFlujosTrabajo();

    RespuestaFlujoTrabajo obtenerFlujoPorId(Long idFlujo);

    RespuestaFlujoTrabajo actualizarFlujoTrabajo(Long idFlujo, PeticionCrearFlujoTrabajo peticion);

    RespuestaFlujoTrabajo agregarEtapa(Long idFlujo, PeticionEtapaConfig peticion);

    RespuestaFlujoTrabajo actualizarEtapa(Long idFlujo, Long idEtapa, PeticionEtapaConfig peticion);

    void eliminarEtapa(Long idFlujo, Long idEtapa);
}
