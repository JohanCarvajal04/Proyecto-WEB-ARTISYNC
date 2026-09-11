package uteq.edu.ec.artisync.service.catalogo;

import org.springframework.data.domain.Page;
import uteq.edu.ec.artisync.dto.peticion.catalogo.*;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.*;

import java.math.BigDecimal;
import java.util.List;

public interface IServicioCatalogoServicio {

    /**
     * Crea un servicio de catálogo para un perfil de creador, validando precio mínimo,
     * propiedad del perfil e identidad verificada antes de publicarlo.
     *
     * @param idPerfilCreador id del perfil creador dueño del servicio
     * @param peticion        datos del servicio a crear (título, precio, subcategorías, etc.)
     * @return el servicio recién creado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil, alguna subcategoría, el flujo, la plantilla de contrato o el cuestionario indicados no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el precio es inválido, el solicitante no es dueño del perfil, la identidad no está verificada o la plantilla de contrato ya no está activa
     */
    RespuestaServicio crearServicio(Long idPerfilCreador, PeticionCrearServicio peticion);

    /**
     * Actualiza los campos de un servicio existente, incluyendo subcategorías y etiquetas si se envían.
     *
     * @param idServicio id del servicio a actualizar
     * @param peticion   campos a modificar; los nulos se dejan sin cambios
     * @return el servicio ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio, alguna subcategoría, el flujo, la plantilla de contrato o el cuestionario indicados no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el precio es inválido, se intenta dejar el servicio sin subcategorías, el solicitante no es dueño del perfil, se publica sin identidad verificada o la plantilla de contrato ya no está activa
     */
    RespuestaServicio actualizarServicio(Long idServicio, PeticionActualizarServicio peticion);

    /**
     * Obtiene el detalle completo de un servicio, incluyendo atributos, etiquetas y subcategorías.
     *
     * @param idServicio id del servicio
     * @return el detalle completo del servicio
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     */
    RespuestaServicio obtenerServicioPorId(Long idServicio);

    /**
     * Elimina un servicio del catálogo junto con sus asociaciones de etiquetas y subcategorías.
     *
     * @param idServicio id del servicio a eliminar
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no es dueño del perfil del servicio
     */
    void eliminarServicio(Long idServicio);

    /**
     * Lista, en formato resumido, los servicios de un creador, opcionalmente filtrados por estado de publicación.
     *
     * @param idPerfilCreador   id del perfil creador
     * @param estadoPublicacion estado a filtrar (p. ej. ACTIVO), o {@code null}/vacío para listar todos
     * @return los servicios del creador que cumplen el filtro
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil creador no existe
     */
    List<RespuestaServicioResumido> listarServiciosPorCreador(Long idPerfilCreador, String estadoPublicacion);

    /**
     * Busca en el catálogo público de servicios activos aplicando filtros combinables de categoría,
     * subcategoría, rango de precio, etiquetas y texto libre.
     *
     * @param categoriaId    id de categoría a filtrar, o {@code null}
     * @param subcategoriaId id de subcategoría a filtrar, o {@code null}
     * @param precioMin      precio mínimo del rango, o {@code null}
     * @param precioMax      precio máximo del rango, o {@code null}
     * @param etiquetaIds    ids de etiquetas a filtrar, o {@code null}
     * @param textoBusqueda  texto libre a buscar en título/descripción, o {@code null}
     * @param sort           criterio de orden solicitado
     * @param page           número de página (base 0)
     * @param size           tamaño de página
     * @return la página de servicios resumidos que cumplen los filtros
     */
    Page<RespuestaServicioResumido> buscarCatalogoServicios(
            Long categoriaId,
            Long subcategoriaId,
            BigDecimal precioMin,
            BigDecimal precioMax,
            List<Long> etiquetaIds,
            String textoBusqueda,
            String sort,
            int page,
            int size);

    /**
     * Lista los atributos dinámicos personalizados asociados a un servicio.
     *
     * @param idServicio id del servicio
     * @return los atributos asignados al servicio
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     */
    List<RespuestaAtributo> listarAtributosPorServicio(Long idServicio);

    /**
     * Agrega un atributo dinámico personalizado a un servicio, reutilizando el atributo global
     * si ya existe uno con el mismo nombre, hasta un máximo de 10 atributos por servicio.
     *
     * @param idServicio id del servicio
     * @param peticion   nombre, tipo de dato y valor asignado del atributo
     * @return el atributo recién asociado al servicio
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no es dueño del perfil, se alcanzó el límite de 10 atributos o el atributo ya está asociado al servicio
     */
    RespuestaAtributo agregarAtributo(Long idServicio, PeticionCrearAtributo peticion);

    /**
     * Actualiza el valor asignado de un atributo ya asociado a un servicio.
     *
     * @param idServicio id del servicio
     * @param idAtributo id de la asociación servicio-atributo a actualizar
     * @param peticion   nuevo valor asignado
     * @return el atributo ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio o la asociación de atributo no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no es dueño del perfil o el atributo no pertenece a este servicio
     */
    RespuestaAtributo actualizarAtributo(Long idServicio, Long idAtributo, PeticionActualizarAtributo peticion);

    /**
     * Elimina un atributo dinámico asociado a un servicio.
     *
     * @param idServicio id del servicio
     * @param idAtributo id de la asociación servicio-atributo a eliminar
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio o la asociación de atributo no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no es dueño del perfil o el atributo no pertenece a este servicio
     */
    void eliminarAtributo(Long idServicio, Long idAtributo);

    /**
     * @param idServicio id del servicio
     * @param idSubcategoria id de la subcategoría a quitar
     * @return el servicio ya actualizado, sin la subcategoría indicada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el servicio se quedaría sin ninguna subcategoria.
     */
    RespuestaServicio quitarSubcategoria(Long idServicio, Long idSubcategoria);

    /**
     * Para moderación: no filtra por estado de publicación (ve borradores, pausados, etc).
     *
     * @param textoBusqueda texto libre a buscar en título/descripción, o {@code null}
     * @param page          número de página (base 0)
     * @param size          tamaño de página
     * @return la página de servicios resumidos, en cualquier estado de publicación
     */
    Page<RespuestaServicioResumido> listarParaModeracion(String textoBusqueda, int page, int size);

    /**
     * Sube el archivo de miniatura de un servicio al almacenamiento configurado.
     *
     * @param archivo archivo de imagen a subir
     * @return la URL pública para usar como urlMiniatura del servicio.
     */
    String subirMiniatura(org.springframework.web.multipart.MultipartFile archivo);
}
