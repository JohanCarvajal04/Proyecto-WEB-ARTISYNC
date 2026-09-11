package uteq.edu.ec.artisync.service.perfil;

import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolioItem;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolioItem;

import java.util.List;

/** Obras que un creador exhibe en su portafolio: imagen o video. */
public interface IPortafolioItemServicio {

    /**
     * Sube una obra nueva al portafolio. Solo el creador dueño del portafolio puede hacerlo.
     *
     * @param idPortafolio id del portafolio destino
     * @param idUsuario    id del usuario que sube la obra, debe ser el dueño del portafolio
     * @param peticion     título y descripción de la obra
     * @param archivo      imagen o video de la obra
     * @return la obra recién creada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no es el dueño del portafolio, o si ya alcanzó el máximo de obras permitido
     */
    RespuestaPortafolioItem subirItem(Long idPortafolio, Long idUsuario,
                                       PeticionCrearPortafolioItem peticion, MultipartFile archivo);

    /**
     * Lista las obras de un portafolio. Un portafolio privado solo lo ve su dueño.
     *
     * @param idPortafolio id del portafolio
     * @param idUsuario    id del usuario consultante, o {@code null} si no hay sesión
     * @return las obras del portafolio
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el portafolio no es público y el solicitante no es su dueño
     */
    List<RespuestaPortafolioItem> listarItems(Long idPortafolio, Long idUsuario);

    /**
     * Obtiene una obra por su id. Si el portafolio contenedor es privado, solo su dueño puede verla.
     *
     * @param idItem    id de la obra
     * @param idUsuario id del usuario consultante, o {@code null} si no hay sesión
     * @return la obra encontrada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la obra no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el portafolio no es público y el solicitante no es su dueño
     */
    RespuestaPortafolioItem obtenerItem(Long idItem, Long idUsuario);

    /**
     * Edita el título y la descripción de una obra. El archivo no cambia: para
     * reemplazarlo hay que eliminar la obra y subir una nueva.
     *
     * @param idItem    id de la obra a editar
     * @param idUsuario id del usuario que edita, debe ser el dueño del portafolio
     * @param peticion  nuevo título y descripción
     * @return la obra ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la obra no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no es el dueño del portafolio
     */
    RespuestaPortafolioItem actualizarItem(Long idItem, Long idUsuario, PeticionCrearPortafolioItem peticion);

    /**
     * Descarga el archivo binario de una obra. Si el portafolio contenedor es privado, solo su dueño puede descargarla.
     *
     * @param idItem    id de la obra
     * @param idUsuario id del usuario consultante, o {@code null} si no hay sesión
     * @return el contenido del archivo, listo para responder al cliente HTTP
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la obra no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el portafolio no es público y el solicitante no es su dueño
     */
    ArchivoItem descargarArchivo(Long idItem, Long idUsuario);

    /**
     * Elimina una obra del portafolio, junto con su archivo almacenado.
     *
     * @param idItem    id de la obra a eliminar
     * @param idUsuario id del usuario que elimina, debe ser el dueño del portafolio
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la obra no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no es el dueño del portafolio
     */
    void eliminarItem(Long idItem, Long idUsuario);

    record ArchivoItem(byte[] contenido, String nombreSugerido, String contentType) {
    }
}
