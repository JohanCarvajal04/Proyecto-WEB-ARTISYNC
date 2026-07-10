package uteq.edu.ec.artisync.service.catalogo;

import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaCategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaSubcategoria;

import java.util.List;

public interface ICategoriaServicio {

    List<RespuestaCategoria> listarCategoriasActivas();

    List<RespuestaCategoria> listarTodasLasCategorias();

    RespuestaCategoria obtenerCategoriaPorId(Long idCategoria);

    RespuestaCategoria crearCategoria(PeticionCrearCategoria peticion);

    RespuestaCategoria actualizarCategoria(Long idCategoria, PeticionActualizarCategoria peticion);

    void eliminarCategoria(Long idCategoria);

    List<RespuestaSubcategoria> listarSubcategoriasPorCategoria(Long idCategoria);

    List<RespuestaSubcategoria> listarTodasLasSubcategorias();

    RespuestaSubcategoria crearSubcategoria(PeticionCrearSubcategoria peticion);

    void eliminarSubcategoria(Long idSubcategoria);
}
