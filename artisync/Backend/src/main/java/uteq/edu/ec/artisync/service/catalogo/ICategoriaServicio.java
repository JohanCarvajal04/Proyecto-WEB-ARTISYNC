package uteq.edu.ec.artisync.service.catalogo;

import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaCategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaSubcategoria;

import java.util.List;

public interface ICategoriaServicio {

    /**
     * Lista las categorías activas, ordenadas alfabéticamente.
     *
     * @return las categorías con estado activo
     */
    List<RespuestaCategoria> listarCategoriasActivas();

    /**
     * Lista todas las categorías, activas e inactivas, ordenadas alfabéticamente.
     *
     * @return todas las categorías del catálogo
     */
    List<RespuestaCategoria> listarTodasLasCategorias();

    /**
     * Obtiene una categoría por su id.
     *
     * @param idCategoria id de la categoría
     * @return la categoría encontrada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría no existe
     */
    RespuestaCategoria obtenerCategoriaPorId(Long idCategoria);

    /**
     * Crea una categoría de catálogo. Si {@code idUsuarioCreador} es nulo, la categoría queda
     * marcada como ya revisada (creada por un administrador); si no, queda pendiente de revisión.
     *
     * @param idUsuarioCreador id del creador que la propone, o {@code null} si la crea un administrador
     * @param peticion         nombre y estado inicial de la categoría
     * @return la categoría recién creada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si {@code idUsuarioCreador} no corresponde a un usuario existente
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya existe una categoría con el mismo nombre
     */
    RespuestaCategoria crearCategoria(Long idUsuarioCreador, PeticionCrearCategoria peticion);

    /**
     * Actualiza el nombre y/o estado activo de una categoría.
     *
     * @param idCategoria id de la categoría a actualizar
     * @param peticion    campos a modificar; los nulos se dejan sin cambios
     * @return la categoría ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el nuevo nombre ya lo usa otra categoría
     */
    RespuestaCategoria actualizarCategoria(Long idCategoria, PeticionActualizarCategoria peticion);

    /**
     * Elimina una categoría, siempre que ninguna de sus subcategorías tenga servicios publicados.
     *
     * @param idCategoria id de la categoría a eliminar
     * @param motivo obligatorio si la categoria la creó un creador (se le notifica).
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si alguna subcategoría tiene servicios publicados, o si la categoría la creó un creador y no se indicó motivo
     */
    void eliminarCategoria(Long idCategoria, String motivo);

    /**
     * Lista las subcategorías de una categoría, ordenadas alfabéticamente.
     *
     * @param idCategoria id de la categoría padre
     * @return las subcategorías de la categoría
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría no existe
     */
    List<RespuestaSubcategoria> listarSubcategoriasPorCategoria(Long idCategoria);

    /**
     * Lista todas las subcategorías del catálogo, ordenadas alfabéticamente.
     *
     * @return todas las subcategorías
     */
    List<RespuestaSubcategoria> listarTodasLasSubcategorias();

    /**
     * Crea una subcategoría dentro de una categoría existente. Si {@code idUsuarioCreador} es nulo,
     * queda marcada como ya revisada; si no, queda pendiente de revisión.
     *
     * @param idUsuarioCreador id del creador que la propone, o {@code null} si la crea un administrador
     * @param peticion         categoría padre y nombre de la subcategoría
     * @return la subcategoría recién creada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría padre no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya existe una subcategoría con el mismo nombre en esa categoría
     */
    RespuestaSubcategoria crearSubcategoria(Long idUsuarioCreador, PeticionCrearSubcategoria peticion);

    /**
     * Elimina una subcategoría, siempre que no tenga servicios publicados.
     *
     * @param idSubcategoria id de la subcategoría a eliminar
     * @param motivo obligatorio si la subcategoria la creó un creador (se le notifica).
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la subcategoría no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si tiene servicios publicados, o si la subcategoría la creó un creador y no se indicó motivo
     */
    void eliminarSubcategoria(Long idSubcategoria, String motivo);

    /**
     * Lista las categorías creadas por creadores que aún no han sido revisadas por un moderador.
     *
     * @return las categorías pendientes de revisión, más recientes primero
     */
    List<RespuestaCategoria> listarCategoriasPendientesRevision();

    /**
     * Lista las subcategorías creadas por creadores que aún no han sido revisadas por un moderador.
     *
     * @return las subcategorías pendientes de revisión, más recientes primero
     */
    List<RespuestaSubcategoria> listarSubcategoriasPendientesRevision();

    /**
     * Marca una categoría como ya revisada por un moderador.
     *
     * @param idCategoria id de la categoría a marcar
     * @return la categoría ya marcada como revisada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría no existe
     */
    RespuestaCategoria marcarCategoriaRevisada(Long idCategoria);

    /**
     * Marca una subcategoría como ya revisada por un moderador.
     *
     * @param idSubcategoria id de la subcategoría a marcar
     * @return la subcategoría ya marcada como revisada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la subcategoría no existe
     */
    RespuestaSubcategoria marcarSubcategoriaRevisada(Long idSubcategoria);
}
