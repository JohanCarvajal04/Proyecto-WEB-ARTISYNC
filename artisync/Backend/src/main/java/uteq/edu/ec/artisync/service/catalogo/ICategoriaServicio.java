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

    RespuestaCategoria crearCategoria(Long idUsuarioCreador, PeticionCrearCategoria peticion);

    RespuestaCategoria actualizarCategoria(Long idCategoria, PeticionActualizarCategoria peticion);

    /** @param motivo obligatorio si la categoria la creó un creador (se le notifica). */
    void eliminarCategoria(Long idCategoria, String motivo);

    List<RespuestaSubcategoria> listarSubcategoriasPorCategoria(Long idCategoria);

    List<RespuestaSubcategoria> listarTodasLasSubcategorias();

    RespuestaSubcategoria crearSubcategoria(Long idUsuarioCreador, PeticionCrearSubcategoria peticion);

    /** @param motivo obligatorio si la subcategoria la creó un creador (se le notifica). */
    void eliminarSubcategoria(Long idSubcategoria, String motivo);

    List<RespuestaCategoria> listarCategoriasPendientesRevision();

    List<RespuestaSubcategoria> listarSubcategoriasPendientesRevision();

    RespuestaCategoria marcarCategoriaRevisada(Long idCategoria);

    RespuestaSubcategoria marcarSubcategoriaRevisada(Long idSubcategoria);
}
