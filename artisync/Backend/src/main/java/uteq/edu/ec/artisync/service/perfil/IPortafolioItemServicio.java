package uteq.edu.ec.artisync.service.perfil;

import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolioItem;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolioItem;

import java.util.List;

/** Obras que un creador exhibe en su portafolio: imagen o video. */
public interface IPortafolioItemServicio {

    RespuestaPortafolioItem subirItem(Long idPortafolio, Long idUsuario,
                                       PeticionCrearPortafolioItem peticion, MultipartFile archivo);

    List<RespuestaPortafolioItem> listarItems(Long idPortafolio, Long idUsuario);

    RespuestaPortafolioItem obtenerItem(Long idItem, Long idUsuario);

    /**
     * Edita el título y la descripción de una obra. El archivo no cambia: para
     * reemplazarlo hay que eliminar la obra y subir una nueva.
     */
    RespuestaPortafolioItem actualizarItem(Long idItem, Long idUsuario, PeticionCrearPortafolioItem peticion);

    ArchivoItem descargarArchivo(Long idItem, Long idUsuario);

    void eliminarItem(Long idItem, Long idUsuario);

    record ArchivoItem(byte[] contenido, String nombreSugerido, String contentType) {
    }
}
