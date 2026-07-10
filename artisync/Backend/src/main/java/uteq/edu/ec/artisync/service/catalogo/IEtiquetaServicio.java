package uteq.edu.ec.artisync.service.catalogo;

import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearEtiqueta;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaEtiqueta;

import java.util.List;

public interface IEtiquetaServicio {

    List<RespuestaEtiqueta> listarEtiquetas();

    RespuestaEtiqueta obtenerPorId(Long idEtiqueta);

    RespuestaEtiqueta crearEtiqueta(PeticionCrearEtiqueta peticion);

    void eliminarEtiqueta(Long idEtiqueta);
}
