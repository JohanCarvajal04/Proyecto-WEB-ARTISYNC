package uteq.edu.ec.artisync.service.catalogo;

import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateTagRequest;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.TagResponse;

import java.util.List;

public interface ITagService {

    /**
     * Lista todas las etiquetas del catálogo.
     *
     * @return las etiquetas registradas
     */
    List<TagResponse> listTags();

    /**
     * Obtiene una etiqueta por su id.
     *
     * @param idEtiqueta id de la etiqueta
     * @return la etiqueta encontrada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la etiqueta no existe
     */
    TagResponse getById(Long idEtiqueta);

    /**
     * Crea una etiqueta nueva.
     *
     * @param peticion nombre de la etiqueta a crear
     * @return la etiqueta recién creada
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya existe una etiqueta con el mismo nombre
     */
    TagResponse createTag(CreateTagRequest peticion);

    /**
     * Elimina una etiqueta del catálogo.
     *
     * @param idEtiqueta id de la etiqueta a eliminar
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la etiqueta no existe
     */
    void deleteTag(Long idEtiqueta);
}
